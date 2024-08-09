package searchengine.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@Service
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmarepository;
    private final IndexRepository indexRepository;
    private LemmaSearch lemmaSearch;
    private ForkJoinPool pool;
    private ExecutorService executor;
    private final Logger log = LoggerFactory.getLogger(IndexingServiceImpl.class);
    private List<Page> pages;
    private volatile boolean flag;

    public IndexingServiceImpl(SitesList sitesList, LemmaRepository lemmaRepository, IndexRepository indexRepository, SiteRepository siteRepository, PageRepository pageRepository, LemmaSearch lemmaSearch) {
        this.sites = sitesList;
        this.lemmarepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaSearch = lemmaSearch;
    }
    @Override
    public IndexingResponse startThread() {
        IndexingResponse response = new IndexingResponse();
        CompletableFuture.runAsync(() -> startIndexing());
        response.setResult(true);
        return response;
    }
    @Override
    public void startIndexing() {
        flag = true;
        long start = System.currentTimeMillis();
        IndexingResponse response = new IndexingResponse();
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        log.info("Запуск индексации - " + sites.getSites().size() + " сайтов.");
        log.info("Переменная - " + MapWebSite.count);
        log.info("Колличество в таблице сайт: " + siteRepository.count());
        log.info("Колличество в таблице страница: " + pageRepository.count());
        addSiteEntity();

        for (Site s : sites.getSites()) {
            executor.execute(() -> {
            pool = new ForkJoinPool();
            System.out.println("=================================================================");
            System.out.println(Thread.currentThread().getName());
            System.out.println("=================================================================");
            System.out.println(pool.isShutdown());
                String url = s.getUrl().contains("www.") ? s.getUrl().replace("www.", "") : s.getUrl();
            SiteEntity siteEntity = siteRepository.findByUrl(url).orElseThrow();
            MapWebSite site = new MapWebSite(url,siteEntity,pageRepository,lemmaSearch);
            pool.invoke(site);
            log.info("Колличество активных потоков после - " + pool.getActiveThreadCount());
            log.info("Статус индексации - " + siteEntity.getStatus());

            if(siteEntity.getStatus().equals(Status.INDEXING)) {
                siteEntity.setStatus(Status.INDEXED);
                siteEntity.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteEntity);
                response.setResult(true);
            } else {
                siteEntity.setStatus(Status.FAILED);
                siteRepository.save(siteEntity);
                response.setResult(false);
                response.setError("Индексация уже запущена, или произошла ошибка индексации!");
            }

                log.info("2 Потоки остановились - " + pool.isShutdown());
                log.info("Переменная count - " + MapWebSite.count);
                log.info("Колличество страниц - " + pageRepository.count());
                pool.shutdown();
                log.info("3 Потоки остановились - " + pool.isShutdown());
                long stop = (System.currentTimeMillis() - start) / 1000;
                log.info("Индексация проведена за - " + stop + " секунд.");
            });
        }
    }
    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse response = new IndexingResponse();
            if (pool != null && !pool.isShutdown() && flag) {
            pool.shutdownNow();
                response.setResult(true);
                String error = "Индексация остановлена пользователем!";
                response.setError(error);

                for (Site s : sites.getSites()) {
                    String url = s.getUrl().contains("www.") ? s.getUrl().replace("www.", "") : s.getUrl();
                    SiteEntity siteEntity = siteRepository.findByUrl(url).orElseThrow();
                    if(siteEntity.getStatus().equals(Status.INDEXED)){
                        continue;
                    }
                    siteEntity.setStatus(Status.FAILED);
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteEntity.setLastError(error);
                    siteRepository.save(siteEntity);
                }

            } else {
                response.setResult(true);
                response.setError("Индексация не запущена!");
            }
        return response;
    }

    @Override
    public void dataBaseClearing(String url) {
        System.out.println("Hello DB clear!");
        Optional<SiteEntity> site = siteRepository.findByUrl(url);
        if (site.isPresent()) {
            System.out.println("DB clear work!");
            siteRepository.deleteByUrl(url);
        }
    }

    @Override
    public void addSiteEntity() {
        for (Site s : sites.getSites()) {
            String url = s.getUrl().contains("www.") ? s.getUrl().replace("www.", "") : s.getUrl();
            dataBaseClearing(url);
            MapWebSite.links.clear();
            MapWebSite.count = 0;
            SiteEntity site = new SiteEntity();
            site.setName(s.getName());
            site.setUrl(url);
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(Status.INDEXING);
            site.setPages(pages);
            siteRepository.save(site);
        }
    }
}
