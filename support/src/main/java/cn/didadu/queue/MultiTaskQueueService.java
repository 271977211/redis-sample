package cn.didadu.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangjing on 16-12-23.
 */

@Service
public class MultiTaskQueueService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 将任务推入队列
     * @param functionName
     * @param args
     */
    public void sendTaskViaQueue(String queueName, String functionName, String ... args){
        StringBuilder task = new StringBuilder();
        task.append("[").append(functionName);
        if(args.length > 0){
            task.append(",[");
            for (String  arg : args) {
                task.append(arg).append(",");
            }
            task.deleteCharAt(task.length() - 1);
            task.append("]");
        }
        task.append("]");
        stringRedisTemplate.opsForList().rightPush(queueName, task.toString());
    }

    /**
     * 处理队列中的任务
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void processTaskQueue(String queueName) throws Exception{
        while (true) {
            String packed = stringRedisTemplate.opsForList().leftPop(queueName, 30l, TimeUnit.SECONDS);
            String functionName;
            String args;
            int index = packed.indexOf(",");
            if(index > 0){
                functionName = packed.substring(1, index);
                args = packed.substring(index + 2, packed.length() - 2);
            }else{
                functionName = packed.substring(1, packed.length() - 1);
                args = "";
            }
            //此处可以用Spring管理Bean
            Class taskClass = Class.forName("cn.didadu.queue." + functionName);
            ITask task = (ITask) taskClass.newInstance();
            task.execute(args.split(","));
        }
    }

    /**
     * 处理队列中的任务
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void processTaskQueue(String[] queueNames) throws Exception{
        while (true) {
            //leftPop参数不支持多个key，需要自己实现下
            byte[][] keys = new byte[queueNames.length][];
            int i = 0;
            for(String queueName : queueNames){
                keys[i++] = queueName.getBytes();
            }
            RedisCallback<List<byte[]>> redisCallback = connection -> connection.bLPop(30, keys);
            String packed = new String(stringRedisTemplate.execute(redisCallback).get(1));

            String functionName;
            String args;
            int index = packed.indexOf(",");
            if(index > 0){
                functionName = packed.substring(1, index);
                args = packed.substring(index + 2, packed.length() - 2);
            }else{
                functionName = packed.substring(1, packed.length() - 1);
                args = "";
            }
            //此处可以用Spring管理Bean
            Class taskClass = Class.forName("cn.didadu.queue." + functionName);
            ITask task = (ITask) taskClass.newInstance();
            task.execute(args.split(","));
        }
    }

}
