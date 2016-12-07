package cn.didadu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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
        final byte[] rawKey = stringRedisTemplate.getStringSerializer().serialize("trans:");
        RedisCallback<List<Object>> pipelineCallback = new RedisCallback<List<Object>>() {
            @Override
            public List<Object> doInRedis(RedisConnection redisConnection) throws DataAccessException {
                redisConnection.openPipeline();
                redisConnection.incr(rawKey);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                redisConnection.decr(rawKey);
                return redisConnection.closePipeline();
            }
        };
        System.out.println(stringRedisTemplate.execute(pipelineCallback).get(0));
    }
}
