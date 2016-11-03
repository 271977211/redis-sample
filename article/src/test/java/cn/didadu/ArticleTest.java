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
public class ArticleTest {
    @Autowired
    private ArticleService articleService;

    @Test
    public void test() throws Exception {
        System.out.println(articleService.postArticle("zhangjing","first article","www.baidu.com"));
        System.out.println(articleService.postArticle("maxiaoxia","second article","www.google.com"));
        System.out.println(articleService.postArticle("dada","third article","www.didazu.cn"));
        articleService.voteArticle("maxiaoxia", "article:1");
        List<Map<String,String>> data = articleService.getArticles(1, "score:");
        System.out.println("");
        articleService.addGroups("myGroup", "article:1");
        articleService.addGroups("myGroup", "article:2");
        articleService.getGroupArticles("group:myGroup");
    }

}
