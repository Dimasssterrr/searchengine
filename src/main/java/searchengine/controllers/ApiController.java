package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingServiceImpl;
import searchengine.services.LemmaSearch;
import searchengine.services.Search;
import searchengine.services.StatisticsService;

@RestController
@ControllerAdvice
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingServiceImpl indexingServiceImpl;
    private final LemmaSearch lemmaSearch;
    private final Search search;

    public ApiController(StatisticsService statisticsService, IndexingServiceImpl indexingServiceImpl, LemmaSearch lemmaSearch, Search search) {
        this.statisticsService = statisticsService;
        this.indexingServiceImpl = indexingServiceImpl;
        this.lemmaSearch = lemmaSearch;
        this.search = search;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingServiceImpl.startThread());
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingServiceImpl.stopIndexing());
    }
    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(lemmaSearch.indexingWebPage(url));
    }
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query,
                                                 @RequestParam(required = false) String site,
                                                 @RequestParam(required = false,defaultValue = "0") int offset,
                                                 @RequestParam(required = false,defaultValue = "10") int limit) {
        return ResponseEntity.ok(search.getResponse(query, site, offset, limit));
    }

}
