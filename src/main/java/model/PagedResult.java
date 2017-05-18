package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class PagedResult<T> {
    // Count of all available results
    @JsonProperty("c")
    private int totalResultCount;

    // Total number of paged results available
    @JsonProperty("pc")
    private int totalPageCount;

    // Current page number
    @JsonProperty("pn")
    private int currentPage;

    // Server Unix timestamp of response
    @JsonProperty("t")
    private long timestamp;

    // An array of result objects that matched the query
    @JsonProperty("results")
    private List<T> results;

    public List<T> getResults() {
        if (results == null) {
            return Collections.emptyList();
        }
        return results;
    }
}
