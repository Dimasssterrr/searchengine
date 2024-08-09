package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.RecursiveAction;

public class MapWebSite extends RecursiveAction {

    private final String link;
    private final SiteEntity site;
    private final PageRepository pageRepository;
    private final LemmaSearch lemmaSearch;
    public static int count = 0;
    public static int countPage = 0;
    public static final LinkedHashSet<String> links = new LinkedHashSet<>();

    private final Logger log = LoggerFactory.getLogger(MapWebSite.class);

    public MapWebSite(String link, SiteEntity site, PageRepository pageRepository, LemmaSearch lemmaSearch) {
        this.link = link;
        this.site = site;
        this.pageRepository = pageRepository;
        this.lemmaSearch = lemmaSearch;

    }
    @Override
    protected void compute() {
        try {
            Thread.sleep(100);
            Connection.Response response = getConnection(link);
            List<MapWebSite> tasks = new ArrayList<>();
            String path = link.replaceFirst(site.getUrl(), "/");
            String content = response.body();
            Page page = new Page();
            page.setSite(site);
            page.setPath(path);
            page.setCode(response.statusCode());
            page.setContent(content);
            synchronized (pageRepository) {
                pageRepository.save(page);
                System.out.println("Страница " + page.getPath() + " добавлена в базу данных!");
                System.out.println("=========================================================================" + "\nСтраница сайта - " + page.getSite().getName() + " Имя страницы - " + page.getPath() + " - Поток  " + Thread.currentThread().getName() + " Колличество страниц - " + countPage++);
                lemmaSearch.createLemma(page,site);
            }
            if (response.statusCode() < 400) {
                Document doc = response.parse();
                Elements elements = doc.select("a");
                for (Element el : elements) {
                    String url = el.attr("abs:href");
                    if (url.equalsIgnoreCase(site.getUrl())) {
                        continue;
                    }
                    if (url.contains(site.getUrl()) && links.add(url) && !url.contains("#") && !url.contains("%") && !url.contains(".jpg") && !url.endsWith("null") && !url.contains(".zip") && !url.contains(".sql") && !url.contains(".pdf")) {
                        count++;
                        MapWebSite task = new MapWebSite(url, site, pageRepository, lemmaSearch);
                        task.fork();
                        tasks.add(task);
                    }
                }
            }
            for (MapWebSite task : tasks) {
                task.join();
            }

        } catch (IOException e) {
            System.err.println("For - [" + link + "] - " + e.getMessage());
            site.setStatus(Status.FAILED);
            site.setLastError(e.getMessage());
            site.setStatusTime(LocalDateTime.now());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection.Response getConnection(String url) throws IOException {
        return Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .timeout(40_000)
                .execute();
    }
}
