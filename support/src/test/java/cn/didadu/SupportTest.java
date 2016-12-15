package cn.didadu;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.List;

/**
 * Created by zhangjing on 16-11-3.
 */

@RunWith(SpringRunner.class)
@SpringBootTest()
public class SupportTest {

    @Autowired
    private LogService logService;

    @Autowired
    private CounterService counterService;

    @Autowired
    private IPService ipService;

    @Autowired
    private RecentContactService recentContactService;

    @Autowired
    private AutoCompleteEmailService autoCompleteEmailService;

    @Autowired
    private FirstLockService firstLockService;

    @Autowired
    private TimeoutLockService timeoutLockService;

    @Test
    public void test() throws Exception {
        //logService.logRecent("test", "this is new log", "INFO");
        //counterService.updateCounter("hits", 1);

        /*List<Pair<Integer,Integer>> result = counterService.getCounter("hits", 60);
        result.forEach(item -> System.out.println(item.getFirst() + "hits: " + item.getSecond()));*/

        //ipService.importIpsToRedis(new File("/home/zhangjing/Desktop/ip/city/GeoLite2-City-Blocks-IPv4.csv"));
        //ipService.importGeonameToRedis(new File("/home/zhangjing/Desktop/ip/city/GeoLite2-City-Locations-zh-CN.csv"));

        //String[] city = ipService.findCityByIp("172.96.113.20");
        //System.out.println(city.toString());

        //ipService.findCityByApi("183.134.104.229");


       /* recentContactService.addUpdateContact("Me", "Jack");
        recentContactService.addUpdateContact("Me", "Tom");
        recentContactService.addUpdateContact("Me", "Jean");
        recentContactService.addUpdateContact("Me", "Jeannie");
        recentContactService.addUpdateContact("Me", "Jeff");

        List<String> list = recentContactService.fetchAutocompleteList("Me", "je");
        System.out.println(list.size());*/

        //autoCompleteEmailService.findPrefixRange("aba");
       /* autoCompleteEmailService.joinGuild("elohim", "jack");
        autoCompleteEmailService.joinGuild("elohim", "jean");
        autoCompleteEmailService.joinGuild("elohim", "jeannie");
        autoCompleteEmailService.joinGuild("elohim", "jeff");
        autoCompleteEmailService.joinGuild("elohim", "jezz");
        autoCompleteEmailService.joinGuild("elohim", "helen");
        autoCompleteEmailService.joinGuild("elohim", "zoo");*/
        //autoCompleteEmailService.autocompleteOnPrefix("elohim", "je");

        //System.out.println(firstLockService.acquireLock("market"));
        //firstLockService.purchaseItemWithLock("1", "1", "1");

        //System.out.println(firstLockService.releaseLock("market", "0147c941-1033-4d22-b6c9-3bdeb84215e8"));
        System.out.println(timeoutLockService.acquireLockWithTimeout("market"));
    }

}
