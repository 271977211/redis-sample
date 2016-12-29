package cn.didadu;

import cn.didadu.queue.DelayTaskQueueService;
import cn.didadu.queue.MultiTaskQueueService;
import cn.didadu.queue.QueueService;
import com.google.common.collect.Sets;
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

    @Autowired
    private SemaphoreService semaphoreService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private MultiTaskQueueService multiTaskQueueService;

    @Autowired
    private DelayTaskQueueService delayTaskQueueService;

    @Autowired
    private ChatService chatService;

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
        //System.out.println(timeoutLockService.acquireLockWithTimeout("market"));

        //System.out.println(semaphoreService.acquireSemaphore("semaphore:remote", 5));

        //System.out.println(semaphoreService.acquireFairSemaphore("semaphore:remote", 5));
        //semaphoreService.releaseFairSemaphore("semaphore:remote", "7edd9a4b-2ade-41e8-82c7-e4229cf22712");
        //System.out.println(semaphoreService.refreshFareSemaphore("semaphore:remote", "aaa"));
        //queueService.sendSoldEmailViaQueue(1, "Item_M", 97, 99);
        //queueService.processEmailQueue();
        //multiTaskQueueService.sendTaskViaQueue("queue:email", "SendEmailTask", "1", "Item_M", "97", "99");
        //multiTaskQueueService.sendTaskViaQueue("queue:message", "SendMessageTask", "bboyjing", "github");
        //multiTaskQueueService.processTaskQueue(new String[]{"queue:email", "queue:message"});

        /*delayTaskQueueService.sendTaskViaQueue("queue:email", "SendEmailTask",  1000 * 30, "1", "Item_M", "97", "99");
        delayTaskQueueService.sendTaskViaQueue("queue:message", "SendMessageTask",  1000 * 60, "bboyjing", "github");
        delayTaskQueueService.processTaskQueue();*/
        //multiTaskQueueService.processTaskQueue(new String[]{"queue:email", "queue:message"});

        chatService.createChat("jason", Sets.newHashSet("jeff", "tom"), "wenlcome to jason's chat");
        chatService.createChat("lily", Sets.newHashSet("jeff", "alice"), "wenlcome to lily's chat");
        chatService.sendMessage("1", "tom", "hi jeff, I'm tom.");
        chatService.fetchPendingMessages("jeff");
    }

}
