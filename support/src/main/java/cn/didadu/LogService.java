package cn.didadu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zhangjing on 16-12-12.
 */

@Service
public class LogService {

    public static final SimpleDateFormat TIMESTAMP =
            new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    private static final SimpleDateFormat ISO_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:00:00");

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void logRecent(String name, String message, String severity) {
        final byte[] destination = stringRedisTemplate
                .getStringSerializer()
                .serialize("recent_" + name + "_" + severity);
        RedisCallback<Object> pipelineCallback = redisConnection -> {
            redisConnection.lPush(destination,
                    (TIMESTAMP.format(new Date()) + ' ' + message).getBytes());
            //只保留最新的100条
            redisConnection.lTrim(destination, 0, 99);
            return null;
        };
        stringRedisTemplate.executePipelined(pipelineCallback);
    }
}
