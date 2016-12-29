package cn.didadu;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Created by zhangjing on 16-12-29.
 */

@Service
public class ChatService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TimeoutLockService timeoutLockService;

    @Autowired
    private FirstLockService firstLockService;

    /**
     * 创建聊天群组
     * @param sender
     * @param recipients
     * @param message
     * @return
     */
    public String createChat(String sender, Set<String> recipients, String message) {
        //通过全局计数器来获取一个新的群组ID
        String chatId = stringRedisTemplate.opsForValue().increment("ids:chat:", 1l).toString();

        //将发送者也添加到群组成员中
        recipients.add(sender);

        stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                recipients.forEach(recipient -> {
                    //将用户添加到群组中，并将这些用户在群组中最大已读消息ID初始化为0
                    stringRedisTemplate.opsForZSet().add("chat:" + chatId, recipient, 0);
                    //将群组ID添加到用户已参加群组的有序集合中
                    stringRedisTemplate.opsForZSet().add("member:" + recipient, chatId,0);
                });
                return operations.exec();
            }
        });

        //发送一条初始化消息
        return sendMessage(chatId, sender, message);
    }

    /**
     * 发送消息
     * @param chatId
     * @param sender
     * @param message
     * @return
     */
    public String sendMessage(String chatId, String sender, String message) {
        // 使用锁来消除竞争条件，保证消息的读取和插入的顺序一致
        String identifier = timeoutLockService.acquireLockWithTimeout("chat:" + chatId);
        if (identifier == null){
            throw new RuntimeException("Couldn't get the lock");
        }
        try{
            //获取消息ID
            Long messageId = stringRedisTemplate.opsForValue().increment("ids:message:" + chatId, 1l);
            //将消息添加到消息有序集合中
            JSONObject values = new JSONObject();
            values.put("id", messageId);
            values.put("ts", System.currentTimeMillis());
            values.put("sender", sender);
            values.put("message", message);
            stringRedisTemplate.opsForZSet().add("msgs:" + chatId, values.toJSONString(), messageId);
        }finally {
            firstLockService.releaseLock("chat:" + chatId, identifier);
        }

        return chatId;
    }

    /**
     * 读取消息
     * @param recipient
     */
    public void fetchPendingMessages(String recipient) {
        // 获取组员的群组ID以及在各组中目前收到的消息的最大ID
        Set<ZSetOperations.TypedTuple<String>> memberSet =
                stringRedisTemplate.opsForZSet().rangeWithScores("member:" + recipient, 0, -1);

        // 获取各聊天组未读消息(分值大于上面获取的最大消息ID)
        List<Object> results = stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                memberSet.forEach(member -> {
                    String chatId = member.getValue();
                    double messageId = member.getScore();
                    operations.opsForZSet().rangeByScore("msgs:" + chatId, ++messageId, Double.MAX_VALUE);
                });
                return operations.exec();
            }
        });

        //遍历未读消息
        stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                int i = 0;
                for(ZSetOperations.TypedTuple<String> member : memberSet){
                    Set<String> messages = (Set<String>) results.get(i++);
                    System.out.println("聊天组：" + member.getValue() + "，有如下未读消息");
                    messages.forEach(message ->
                            System.out.println(JSONObject.parseObject(message).getString("message")));
                    //修改群组成员读取的最大消息ID
                    operations.opsForZSet().incrementScore(
                            "member:" + recipient,
                            member.getValue(),
                            messages.size());
                    //修改群组有序集合中成员读取的最大消息ID
                    operations.opsForZSet().incrementScore(
                            "chat:" + member.getValue(),
                            recipient,
                            messages.size());
                }
                return operations.exec();
            }
        });
    }


}
