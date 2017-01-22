package cn.didadu;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * Created by zhangjing on 16-11-3.
 */

@RunWith(SpringRunner.class)
@SpringBootTest()
public class SearchTest {
    @Autowired
    private SearchService searchService;

    @Test
    public void test() throws Exception {
       /* String docA = "this is some random content, look at how it is indexed.";
        searchService.indexDocument("docA", docA);
        String docB = "this is some random content, look at how it is indexed. And now we are indexing another content";
        searchService.indexDocument("docB", docB);

        searchService.intersect("content", 30, "indexing");
        searchService.union("content", 30, "indexing");
        searchService.difference("content", 30, "indexing");

        searchService.parse("content indexed +indexing -the");

        System.out.println(searchService.parseAndSearch("content indexed +indexing -another", 300));
*/
        List<String> result = searchService.searchAndSort("content indexed +indexing ", "-id").getResults();
        result.forEach(item -> System.out.println(item));
    }

}
