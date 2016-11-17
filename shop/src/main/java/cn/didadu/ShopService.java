package cn.didadu;

import javafx.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * Created by zhangjing on 16-11-3.
 */

@Service
public class ShopService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 确认登录情况
     *
     * @param token
     * @return
     */
    public String checkToken(String token) {
        return (String) redisTemplate.opsForHash().get("login:", token);
    }

    /**
     * 更新令牌
     *
     * @param token
     * @param user
     * @param item
     */
    public void updateToken(String token, String user, String item) {
        long timestamp   = System.currentTimeMillis() / 1000;
        //记录登录用户令牌
        redisTemplate.opsForHash().put("login:", token, user);
        //记录最近登陆用户
        redisTemplate.opsForZSet().add("recent:", token, timestamp);
        if (!StringUtils.isEmpty(item)) {
            //记录用户浏览过的商品
            redisTemplate.opsForZSet().add("viewed:" + token, item, timestamp);
            redisTemplate.opsForZSet().removeRange("viewed:" + token, 0, -26);
        }
    }

    /**
     * 修改购物车
     *
     * @param token
     * @param item
     * @param count
     */
    public void addToCart(String token, String item, int count) {
        if (count <= 0) {
            redisTemplate.opsForHash().delete("cart:" + token, item);
        } else {
            redisTemplate.opsForHash().put("cart:" + token, item, String.valueOf(count));
        }
    }

    /**
     * 缓存请求信息
     *
     * @param request
     * @param callback
     * @return
     */
    public String cacheRequest(String request, Callback<String, String> callback) {
        String pageKey = "cache:" + request.hashCode();
        //获取网页缓存内容
        String content = (String) redisTemplate.opsForValue().get(pageKey);
        if (content == null && callback != null) {
            content = callback.call(request);
            //缓存网页内容并且设置过期时间
            redisTemplate.opsForValue().set(pageKey, content);
            redisTemplate.expire(pageKey, 300, TimeUnit.SECONDS);
        }
        return content;
    }

    /**
     * 缓存商品数量
     */
    public void cacheData(){
        redisTemplate.opsForValue().set("product:001",5);
        while (true){
            long stock = redisTemplate.opsForValue().increment("product:001", -1);
            if(stock >= 0){
                System.out.println("sold one product, remain " + stock);
            }else{
                break;
            }
        }
    }
}
