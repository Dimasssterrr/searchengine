package searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@lombok.Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {
    private boolean result;
    private Integer count;
    private List<SearchData> data;
    private String error;

}
