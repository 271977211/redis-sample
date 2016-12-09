package cn.didadu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by zhangjing on 16-12-6.
 */

@Service
public class TransactionService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void incr() throws InterruptedException {
        List<Object> results = stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForValue().increment("trans:", 1);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                operations.opsForValue().increment("trans:", -1);
                return operations.exec();
            }
        });
        System.out.println(results.get(0));
    }

    public void incrByPipeline() throws InterruptedException{
        final byte[] rawKey = stringRedisTemplate.getStringSerializer().serialize("trans:");
        RedisCallback<Object> pipelineCallback = redisConnection -> {
            redisConnection.incr(rawKey);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            redisConnection.decr(rawKey);
            return null;
        };
        System.out.println(stringRedisTemplate.executePipelined(pipelineCallback).get(0));
    }
}