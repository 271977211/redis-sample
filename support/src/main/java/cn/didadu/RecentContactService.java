package cn.didadu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by zhangjing on 16-12-14.
 */

@Service
public class RecentContactService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 添加或修改联系人
     * @param user
     * @param contact
     */
    public void addUpdateContact(String user, String contact) {
        String acList = "recent:" + user;
        stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                //移除列表中所有值为contact的元素。
                operations.opsForList().remove(acList, 0, contact);
                //将联系人添加到列表最前面
                operations.opsForList().leftPush(acList, contact);
                //保留别表最前面100个元素
                operations.opsForList().trim(acList, 0, 99);
                return operations.exec();
            }
        });
    }

    /**
     * 删除联系人
     * @param user
     * @param contact
     */
    public void removeContact(String user, String contact){
        stringRedisTemplate.opsForList().remove("recent:" + user, 0, contact);
    }

    /**
     * 获取匹配的联系人列表
     * @param user
     * @param prefix
     * @return
     */
    public List<String> fetchAutocompleteList(String user, String prefix) {
        List<String> candidates = stringRedisTemplate.opsForList().range("recent:" + user, 0, -1);
        List<String> matches = candidates.stream().
                filter(candidate -> candidate.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
        return matches;
    }
}
