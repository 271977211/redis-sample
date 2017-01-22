package cn.didadu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.SortParameters;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.query.SortQuery;
import org.springframework.data.redis.core.query.SortQueryBuilder;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhangjing on 17-1-18.
 */

@Service
public class SearchService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 初始化非用词
     */
    private static final Set<String> STOP_WORDS = new HashSet();

    private static final Pattern WORDS_RE = Pattern.compile("[a-z']{2,}");

    private static final Pattern QUERY_RE = Pattern.compile("[+-]?[a-z']{2,}");

    static {
        for (String word :
                ("able about across after all almost also am among " +
                        "an and any are as at be because been but by can " +
                        "cannot could dear did do does either else ever " +
                        "every for from get got had has have he her hers " +
                        "him his how however if in into is it its just " +
                        "least let like likely may me might most must my " +
                        "neither no nor not of off often on only or other " +
                        "our own rather said say says she should since so " +
                        "some than that the their them then there these " +
                        "they this tis to too twas us wants was we were " +
                        "what when where which while who whom why will " +
                        "with would yet you your").split(" "))
        {
            STOP_WORDS.add(word);
        }
    }

    /**
     *  标记化
     * @param content
     * @return
     */
    public Set<String> tokenize(String content) {
        Set<String> words = new HashSet<>();
        Matcher matcher = WORDS_RE.matcher(content);
        while (matcher.find()){
            String word = matcher.group().trim();
            if (word.length() > 2 && !STOP_WORDS.contains(word)){
                words.add(word);
            }
        }
        return words;
    }

    /**
     * 索引文档
     * @param docid
     * @param content
     * @return
     */
    public int indexDocument(String docid, String content) {
        //获取标记化并且去非用词后的单词
        Set<String> words = tokenize(content);
        List<Object> results = stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                for (String word : words) {
                    operations.opsForSet().add("idx:" + word, docid);
                }
                return operations.exec();
            }
        });
        return results.size();
    }

    /**
     * 根据method，对集合进行交集、并集和差集计算并临时保存
     * 30秒后过期
     * @param method
     * @param ttl
     * @param key
     * @param otherKeys
     * @return
     */
    private String setCommon(String method, int ttl, String key, String... otherKeys) throws Exception {
        // 组装其他set的key
        List<String> otherKeyList = new ArrayList<>();
        for(String otherKey : otherKeys){
            otherKeyList.add("idx:" + otherKey);
        }

        //生成临时标识符
        String id = UUID.randomUUID().toString();
        String destKey = "idx:" + id;

        //反射调用指定的方法
        Method[] methods = stringRedisTemplate.opsForSet().getClass().getMethods();
        for (Method m : methods){
            if(m.getName().equals(method)){
                Class[] parameterTypes = m.getParameterTypes();
                if(parameterTypes[1].getName().equals("java.util.Collection")){
                    m.setAccessible(true);
                    //反射调用方法
                    m.invoke(stringRedisTemplate.opsForSet(), "idx:" + key, otherKeyList, destKey);
                    //30秒后过期
                    stringRedisTemplate.expire(destKey, ttl, TimeUnit.SECONDS);
                    break;
                }
            }
        }
        return id;
    }

    /**
     * 集合求交集
     * @param key
     * @param otherKeys
     * @return
     * @throws Exception
     */
    public String intersect(String key, int ttl, String... otherKeys) throws Exception {
        return setCommon("intersectAndStore", ttl, key, otherKeys);
    }

    /**
     * 集合求并集
     * @param key
     * @param otherKeys
     * @return
     * @throws Exception
     */
    public String union(String key, int ttl, String... otherKeys) throws Exception {
        return setCommon("unionAndStore", ttl, key, otherKeys);
    }

    /**
     * 集合求差集
     * @param key
     * @param otherKeys
     * @return
     * @throws Exception
     */
    public String difference(String key, int ttl,String... otherKeys) throws Exception {
        return setCommon("differenceAndStore", ttl, key, otherKeys);
    }

    /**
     * 对查询字符串进行语法分析
     * @param queryString
     * @return
     */
    public Query parse(String queryString) {
        Query query = new Query();
        //查询单词集合
        Set<String> current = new HashSet<>();
        Matcher matcher = QUERY_RE.matcher(queryString.toLowerCase());

        while (matcher.find()){
            String word = matcher.group().trim();
            //获取前缀，如果有则去掉
            char prefix = word.charAt(0);
            if (prefix == '+' || prefix == '-') {
                word = word.substring(1);
            }
            //验证单词合法性
            if (word.length() < 2 || STOP_WORDS.contains(word)) {
                continue;
            }

            //若前缀为'-'，表示想查询不包含该单词的文档
            if (prefix == '-') {
                query.unwanted.add(word);
                continue;
            }

            /**
             * 如果同义词集合非空，并且单词不带'+'前缀
             * 创建查询单词集合
             */
            if (!current.isEmpty() && prefix != '+') {
                query.all.add(new ArrayList<>(current));
                current.clear();
            }
            current.add(word);
        }

        if (!current.isEmpty()){
            query.all.add(new ArrayList<>(current));
        }
        return query;
    }


    /**
     * 语法分析并查询
     * @param queryString
     * @return
     */
    public String parseAndSearch(String queryString, int ttl) throws Exception {
        Query query = parse(queryString);
        if (query.all.isEmpty()){
            return "";
        }

        List<String> toIntersect = new ArrayList<>();
        for (List<String> syn : query.all) {
            if(syn.size() > 1){
                //如果查询单词列表有多个，则先求并集
                String key = syn.get(0);
                syn.remove(0);
                String[] otherKeys = new String[syn.size()];
                toIntersect.add(union(key, ttl, syn.toArray(otherKeys)));
            }else {
                //如果查询单词列表只包含一个，则直接使用
                toIntersect.add(syn.get(0));
            }
        }

        //交集计算结果
        String intersectResult;
        if (toIntersect.size() > 1) {
            //求交集
            String key = toIntersect.get(0);
            toIntersect.remove(0);
            String[] otherKeys = new String[toIntersect.size()];
            intersectResult = intersect(key, ttl, toIntersect.toArray(otherKeys));
        }else {
            intersectResult = toIntersect.get(0);
        }

        //求差集
        if (!query.unwanted.isEmpty()) {
            intersectResult = difference(intersectResult, ttl,
                    query.unwanted.toArray(new String[query.unwanted.size()]));
        }

        return intersectResult;
    }


    /**
     * 查询并排序
     * @param queryString
     * @param sort
     * @return
     * @throws Exception
     */
    public SearchResult searchAndSort(String queryString, String sort) throws Exception {
        //判断是否降序(默认升序)
        boolean desc = sort.startsWith("-");
        if (desc){
            sort = sort.substring(1);
        }
        //如果不是以时间或者id排序，则根据字母表顺序对元素进行排序
        boolean alpha = !"updated".equals(sort) && !"id".equals(sort);
        //定义排序权重
        String by = "kb:doc*->" + sort;
        //获取搜索后的缓存记录
        String id = parseAndSearch(queryString, 300);

        List<Object> results = stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                //获取搜索结果的数量
                operations.opsForSet().size("idx:" + id);
                //构造排序参数
                SortQuery<String> sortQuery = SortQueryBuilder.sort("idx:" + id)
                        .by(by)
                        .limit(0, 20)
                        .alphabetical(alpha)
                        .order(desc ? SortParameters.Order.DESC : SortParameters.Order.ASC).build();
                operations.sort(sortQuery);
                return operations.exec();
            }
        });
        return new SearchResult(
                id,
                ((Long)results.get(0)).longValue(),
                (List<String>)results.get(1));
    }
}
