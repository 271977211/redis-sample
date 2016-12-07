package cn.didadu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Created by zhangjing on 16-12-6.
 */

@Service
public class NoTransactionService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void incr() throws InterruptedException {
        //自增操作
        System.out.println(redisTemplate.opsForValue().increment("notrans:", 1));
        //休眠100毫秒
        Thread.sleep(100);
        //自减操作
        redisTemplate.opsForValue().increment("notrans:", -1);
    }
}
