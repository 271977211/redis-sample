package cn.didadu;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by zhangjing on 16-11-3.
 */

@RunWith(SpringRunner.class)
@SpringBootTest()
public class MarketTest {
    @Autowired
    private MarketService marketService;

    /**
     * 启动三个线程
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        //System.out.println(marketService.listItem("ItemM", "001", 9700l));
        System.out.println(marketService.purchaseItem("002", "ItemM", "001"));
    }

}
