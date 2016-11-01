package cn.didadu.article;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

/**
 * Created by zhangjing on 16-10-28.
 */

@RunWith(SpringRunner.class)
@SpringBootTest()
public class ArticleTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ArticleService articleService;

    @Test
    public void test() throws Exception {
        // 保存字符串
        /*stringRedisTemplate.opsForValue().set("first-key", "zhangjing");
        Assert.assertEquals("zhangjing", stringRedisTemplate.opsForValue().get("first-key"));*/
        //articleService.postArticle();
        //articleService.postHash();
        //Article article = articleService.getArticle();
        //System.out.println(article.getLink());
       //System.out.println(articleService.getTitle());
       /* System.out.println(articleService.postArticle("zhangjing","first article","www.baidu.com"));
        System.out.println(articleService.postArticle("maxiaoxia","second article","www.google.com"));
        System.out.println(articleService.postArticle("dada","third article","www.didazu.cn"));
        articleService.getArticle("article:3");*/
        //articleService.voteArticle("maxiaoxia", "article:1");
        /*List<Map<String,String>> data = articleService.getArticles(1, "score:");
        System.out.println("");*/
       /* articleService.addGroups("myGroup", "article:1");
        articleService.addGroups("myGroup", "article:2");*/
        articleService.getGroupArticles("group:myGroup");
    }
}
