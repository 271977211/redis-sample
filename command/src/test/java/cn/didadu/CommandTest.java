package cn.didadu;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

/**
 * Created by zhangjing on 16-11-3.
 */

@RunWith(SpringRunner.class)
@SpringBootTest()
public class CommandTest {
    @Autowired
    private NoTransactionService noTransactionService;

    @Autowired
    private TransactionService transactionService;

    /**
     * 启动三个线程
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        for(int i = 0; i < 3; i++){
           new Thread(()->{
               try {
                   transactionService.incr();
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }).start();
        }
        Thread.sleep(Integer.MAX_VALUE);
    }

}
