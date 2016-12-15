package cn.didadu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Created by zhangjing on 16-12-15.
 */

@Service
public class TimeoutLockService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    public String acquireLockWithTimeout(String lockName){
        //10秒内获取不到锁就返回
        long acquireTimeout = 10000;
        //20秒后锁超时
        long lockTimeout = 20000;
        String identifier = UUID.randomUUID().toString();
        final byte[] rawKey = stringRedisTemplate.getStringSerializer().serialize("lock:" + lockName);
        final byte[] rawVal = stringRedisTemplate.getStringSerializer().serialize(identifier);
        long end = System.currentTimeMillis() + acquireTimeout;
        while (System.currentTimeMillis() < end) {
            RedisCallback<Boolean> redisCallback = redisConnection ->{
                //成功获取锁之后设置锁超时时间
                if(redisConnection.setNX(rawKey, rawVal)){
                    redisConnection.expire(rawKey, lockTimeout);
                    return true;
                }
                /**
                 * 锁获取失败之后检测锁是否有超时时间，如果没有则设置超时时间
                 * 这是为了防止程序在setNX和expire之间崩溃
                 */
                if(redisConnection.ttl(rawKey) == -1){
                    redisConnection.expire(rawKey, lockTimeout);
                    return false;
                }
                return false;
            };
            if(stringRedisTemplate.execute(redisCallback).booleanValue()){
                return identifier;
            }
        }
        return null;
    }

}
