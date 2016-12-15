package cn.didadu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Created by zhangjing on 16-12-15.
 */

@Service
public class FirstLockService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取锁
     * @param lockName
     * @return
     */
    public String acquireLock(String lockName){
        long acquireTimeout = 10000;
        //随机128位UUID作为键的值
        String identifier = UUID.randomUUID().toString();
        final byte[] rawKey = stringRedisTemplate.getStringSerializer().serialize("lock:" + lockName);
        final byte[] rawVal = stringRedisTemplate.getStringSerializer().serialize(identifier);
        long end = System.currentTimeMillis() + acquireTimeout;
        //10秒内获取不到锁就返回
        while (System.currentTimeMillis() < end){
            RedisCallback<Boolean> redisCallback = redisConnection ->
                    /**
                     * setnx命令的语义是将key的值设为value，当且仅当key不存在
                     * 若key存在，不做任何动作，返回0(false)
                     */
                    redisConnection.setNX(rawKey, rawVal);
           if(stringRedisTemplate.execute(redisCallback).booleanValue()){
               return identifier;
           }
        }
        return null;
    }

    /**
     * 加锁版购买商品
     * @param buyerId
     * @param itemId
     * @param sellerId
     * @return
     */
    public boolean purchaseItemWithLock(String buyerId, String itemId, String sellerId){
        String locked = acquireLock("market");
        if(locked == null){
            return false;
        }else{
            try{
                System.out.println("purchasing....");
                return true;
            }finally {
                //release lock
                System.out.println("release lock");
                releaseLock("market", locked);
            }
        }
    }

    public boolean releaseLock(String lockName, String identifier) {
        String lockKey = "lock:" + lockName;
        SessionCallback<List<Object>> sessionCallback =  new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.watch(lockKey);
                if(identifier.equals(operations.opsForValue().get(lockKey))){
                    operations.multi();
                    operations.delete(lockKey);
                    return operations.exec();
                }
                //若取出来的不是想要释放的锁，不作任何操作，返回空
                operations.unwatch();
                return null;
            }
        };
        List<Object> results = stringRedisTemplate.execute(sessionCallback);
        return results != null ? true : false;
    }

}
