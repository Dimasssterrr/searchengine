package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    IndexingResponse startThread();
    void startIndexing();
    IndexingResponse stopIndexing();
    void dataBaseClearing(String url);
    void addSiteEntity();


}
