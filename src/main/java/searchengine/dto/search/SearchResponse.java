package searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@lombok.Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {
    private boolean result;
    private Integer count;
    private List<SearchData> data;
    private String error;

    public SearchResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
