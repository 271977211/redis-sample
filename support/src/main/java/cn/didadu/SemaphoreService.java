package cn.didadu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Created by zhangjing on 16-12-16.
 */

@Service
public class SemaphoreService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TimeoutLockService timeoutLockService;

    @Autowired
    private FirstLockService firstLockService;

    /**
     * 获取信号量
     * @param semname
     * @param limit
     * @return
     */
    public String acquireSemaphore(String semname, int limit){
        //信号量多久后超时
        long timeout = 100000;

        String identifier = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        SessionCallback<List<Object>> sessionCallback =  new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                /**
                 * 移除有序集合中score介于min和max之间的成员
                 * 清理有序集合中时间戳大于超时数字的标识符
                 */
                operations.opsForZSet().removeRangeByScore(semname, Double.MIN_VALUE, now - timeout);
                //尝试获取信号量
                operations.opsForZSet().add(semname, identifier, now);
                operations.opsForZSet().rank(semname, identifier);
                return operations.exec();
            }
        };
        //检查是否成功获取了信号量
        if((Long) stringRedisTemplate.execute(sessionCallback).get(2) < limit){
            return identifier;
        }
        //获取信号量失败，删除之前添加的标示
        stringRedisTemplate.opsForZSet().remove(semname, identifier);
        return null;
    }

    /**
     * 释放信号量
     * @param semname
     * @param identifier
     */
    public void releaseSemaphore(String semname, String identifier){
        stringRedisTemplate.opsForZSet().remove(semname, identifier);
    }

    /**
     * 公平信号量
     * @param semname
     * @param limit
     * @return
     */
    public String acquireFairSemaphore(String semname, int limit){
        //信号量多久后超时
        long timeout = 100000;

        String identifier = UUID.randomUUID().toString();
        String czset = semname + ":owner";
        String ctr = semname + ":counter";
        long now = System.currentTimeMillis();

        RedisCallback<List<Object>> redisCallback = redisConnection -> {
            redisConnection.multi();
            //清理有序集合中时间戳大于超时数字的标识符
            redisConnection.zRemRangeByScore(semname.getBytes(), Double.MIN_VALUE, now - timeout);
            /**
             * czset、semname求交集，并将结果集覆盖到czset中，目的也是清理czset中的过期信号量
             * 由于RedisOperations中的intersectAndStore方法没有weights参数，所以用这里是使用RedisConnection
             * czset * 1 与 semname * 0求交集，这样就只留下了czset中的数据
             */
            redisConnection.zInterStore(czset.getBytes(),
                    RedisZSetCommands.Aggregate.SUM,
                    new int[]{1,0}, czset.getBytes(),
                    semname.getBytes());
            //信号量计数+1
            redisConnection.incr(ctr.getBytes());
            return redisConnection.exec();
        };
        //获取计数器
        long counter = (Long)stringRedisTemplate.execute(redisCallback).get(2);

        /**
         * 尝试获取信号量
         */
        redisCallback = redisConnection -> {
            redisConnection.multi();
            redisConnection.zAdd(semname.getBytes(), now, identifier.getBytes());
            redisConnection.zAdd(czset.getBytes(), counter, identifier.getBytes());
            redisConnection.zRank(czset.getBytes(), identifier.getBytes());
            return redisConnection.exec();
        };

        if((Long)stringRedisTemplate.execute(redisCallback).get(2) < limit){
            return identifier;
        }

        /**
         * 获取信号量失败，删除记录
         */
        redisCallback = redisConnection -> {
            redisConnection.multi();
            redisConnection.zRem(semname.getBytes(), identifier.getBytes());
            redisConnection.zRem(czset.getBytes(), identifier.getBytes());
            return redisConnection.exec();
        };
        stringRedisTemplate.execute(redisCallback);

        return null;
    }

    /**
     * 释放公平信号量
     * @param semname
     * @param identifier
     * @return
     */
    public boolean releaseFairSemaphore(String semname, String identifier){
        SessionCallback<List<Object>> sessionCallback =  new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForZSet().remove(semname, identifier);
                operations.opsForZSet().remove(semname + ":owner", identifier);
                return operations.exec();
            }
        };
        return (Long)stringRedisTemplate.execute(sessionCallback).get(1) == 1;
    }

    /**
     * 更新信号量
     * @param semname
     * @param identifier
     * @return
     */
    public boolean refreshFareSemaphore(String semname, String identifier){
        /**
         * zadd返回被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员。
         * add返回true表示添加了新成员，而不是更新，所以需要释放掉刚添加的数据
         */
        if(stringRedisTemplate.opsForZSet().add(semname, identifier, System.currentTimeMillis())){
            releaseFairSemaphore(semname, identifier);
        }
        return false;
    }

    /**
     * 加锁的信号量实现
     * @param semname
     * @param limit
     * @return
     */
    public String acquireSemaphoreWithLock(String semname, int limit){
        String identifier = timeoutLockService.acquireLockWithTimeout(semname);
        if(!StringUtils.isEmpty(identifier)){
            try{
                return acquireFairSemaphore(semname, limit);
            }finally {
                firstLockService.releaseLock(semname, identifier);
            }
        }
        return null;
    }
}
