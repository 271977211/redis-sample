package cn.didadu.queue;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created by zhangjing on 16-12-22.
 */

@Service
public class QueueService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 将信息推入邮件列表
     * @param seller
     * @param item
     * @param price
     * @param buyer
     */
    public void sendSoldEmailViaQueue(int seller, String item, int price, int buyer){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("seller_id", seller);
        jsonObject.put("item_id", item);
        jsonObject.put("price", price);
        jsonObject.put("buyer_id", buyer);
        jsonObject.put("time", System.currentTimeMillis());
        stringRedisTemplate.opsForList().rightPush("queue:email", jsonObject.toJSONString());
    }

    /**
     * 处理待发邮件
     */
    public void processEmailQueue(){
        while (true) {
            /**
             * 给了超时参数的leftPop方法调用了bLPop命令
             * 当给定列表内没有任何元素可供弹出的时候，连接将被 BLPOP 命令阻塞，直到等待超时或发现可弹出元素为止。
             */
            String packed = stringRedisTemplate.opsForList().leftPop("queue:email", 30l, TimeUnit.SECONDS);
            JSONObject jsonObject = JSONObject.parseObject(packed);
            System.out.println(jsonObject.getString("item_id"));
        }
    }
}
