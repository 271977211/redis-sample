package cn.didadu.queue;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Created by zhangjing on 16-12-26.
 */

@Service
public class DelayTaskQueueService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MultiTaskQueueService multiTaskQueueService;

    /**
     * 添加到延迟任务列表
     * @param queueName
     * @param functionName
     * @param args
     */
    public String sendTaskViaQueue(String queueName, String functionName, long delay,String ... args){
        String task_id = UUID.randomUUID().toString();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("task_id", task_id);
        jsonObject.put("queue_name", queueName);
        jsonObject.put("function_name", functionName);
        jsonObject.put("args", args);
        stringRedisTemplate.opsForZSet().add(
                "delayed:",
                jsonObject.toJSONString(),
                System.currentTimeMillis() + delay);
        return task_id;
    }

    /**
     * 处理延迟任务队列
     * @throws Exception
     */
    public void processTaskQueue() throws Exception{
        while (true) {
            //获取延迟任务列表中的第一个任务
            Set<ZSetOperations.TypedTuple<String>> delayTaskSet =
                    stringRedisTemplate.opsForZSet().rangeWithScores("delayed:", 0, 0);
            //Redis没有直接提供阻塞有序集合的方法，需要自己检测
            if(delayTaskSet.size() == 0){
                Thread.sleep(1000);
                continue;
            }
            //获取任务信息
            String delayTaskStr = "";
            long delay = 0;
            for(ZSetOperations.TypedTuple typedTuple : delayTaskSet){
                delayTaskStr = (String) typedTuple.getValue();
                delay = typedTuple.getScore().longValue();
                break;
            }
            //若还未到执行时间，等待一会儿继续loop
            if(delay > System.currentTimeMillis()){
                Thread.sleep(1000);
                continue;
            }
            //将到执行时间的任务推入适当的任务队列中，并删除记录
            JSONObject jsonObject = JSONObject.parseObject(delayTaskStr);
            JSONArray jsonArray = jsonObject.getJSONArray("args");
            int argsSize = jsonArray == null ? 0 : jsonArray.size();
            String[] args = new String[argsSize];
            for (int i = 0; i <jsonArray.size(); i++){
                args[i] = jsonArray.getString(i);
            }
            multiTaskQueueService.sendTaskViaQueue(
                    jsonObject.getString("queue_name"),
                    jsonObject.getString("function_name"),
                    args);
            stringRedisTemplate.opsForZSet().remove("delayed:", delayTaskStr);
        }
    }

}
