package searchengine.dto.search;

import org.springframework.stereotype.Component;

@lombok.Data
@Component
public class SearchData {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Float relevance;
}
