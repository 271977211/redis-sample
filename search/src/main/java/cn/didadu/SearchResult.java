package cn.didadu;

import java.util.List;

/**
 * Created by zhangjing on 17-1-20.
 */
public class SearchResult {
    public final String id;
    public final long total;
    public final List<String> results;

    public SearchResult(String id, long total, List<String> results) {
        this.id = id;
        this.total = total;
        this.results = results;
    }

    public String getId() {
        return id;
    }

    public long getTotal() {
        return total;
    }

    public List<String> getResults() {
        return results;
    }
}
