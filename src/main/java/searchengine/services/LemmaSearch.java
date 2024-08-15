package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@Component
public class LemmaSearch {
    private final String[] partsName = {"СОЮЗ", "ПРЕДЛ", "МЕЖД", "ЧАСТ"};
    private final LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private static int countLemma = 0;
    private final Logger log = LoggerFactory.getLogger(LemmaSearch.class);

    public LemmaSearch(PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) throws IOException {
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    public HashMap<String, Integer> getLemmas(String text) {
        String[] words = text.toLowerCase(Locale.ROOT).replaceAll("ё", "е")
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (wordBaseForms.stream().anyMatch(this::hasParticleProperty)) {
                continue;
            }
            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }
            String lemma = normalForms.get(0);
            Integer integer = lemmas.containsKey(lemma) ? lemmas.put(lemma, lemmas.get(lemma) + 1) : lemmas.put(lemma, 1);
        }
        return lemmas;
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : partsName) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    public String pageContentClean(Page page) {
        return Jsoup.parse(page.getContent()).text();
    }

    public IndexingResponse indexingWebPage(String url) {
        IndexingResponse indexingResponse = new IndexingResponse();
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            Connection.Response response = MapWebSite.getConnection(url);
            String content = response.body();
            System.out.println(path);
            Page page = pageRepository.findByPath(path);
            if (page != null) {
                SiteEntity siteEntity = page.getSite();
                indexingResponse.setResult(true);
                log.info("Количество страниц: " + pageRepository.count());
                log.info("Id страницы - " + page.getId());
                List<Lemma> lemmas = lemmaRepository.findAllWherePageId(page.getId());
                lemmas.forEach(e -> {
                    e.setFrequency(e.getFrequency() - 1);
                    lemmaRepository.save(e);
                });
                lemmas.stream().sorted((e1, e2) -> e1.getId().compareTo(e2.getId())).forEach(e -> System.out.println(e.getId() + ", " + e.getLemma() + ", " + e.getFrequency() + ", " + e.getSiteId().getId()));
                pageRepository.deleteById(page.getId());
                log.info("Колличество страниц после удаления: " + pageRepository.count());
                Page newPage = new Page();
                newPage.setSite(siteEntity);
                newPage.setContent(content);
                newPage.setPath(path);
                newPage.setCode(response.statusCode());
                pageRepository.save(newPage);
                log.info("Колличество страниц после добавления: " + pageRepository.count());
                createLemma(newPage, siteEntity);
            } else {
                indexingResponse.setResult(false);
                indexingResponse.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return indexingResponse;
    }

    @Transactional
    public void createLemma(Page page, SiteEntity site) {
        List<Lemma> lemmas = new ArrayList<>();
        List<Index> indexes = new ArrayList<>();
        String text = pageContentClean(page);
        synchronized (lemmaRepository) {
            synchronized (indexRepository) {
                getLemmas(text).forEach((e1, e2) -> {
                    Lemma oldLemma = lemmaRepository.findByLemmaAndSiteId(e1, site);
                    if (oldLemma == null) {
                        countLemma++;
                        log.info("Начало создания новой лемы страницы - " + page.getPath());
                        Lemma lemma = new Lemma();
                        lemma.setLemma(e1);
                        lemma.setSiteId(site);
                        lemma.setFrequency(1);
                        lemmas.add(lemma);
                        log.info("Новая лемма - " + lemma.getLemma() + " " + lemma.getFrequency());

                        Index index = new Index();
                        index.setLemmaId(lemma);
                        index.setPageId(page);
                        index.setRank((float) e2);
                        indexes.add(index);
                    } else {
                        oldLemma.setFrequency(oldLemma.getFrequency() + 1);
                        log.info("Старая лемма - " + oldLemma.getLemma() + " " + oldLemma.getFrequency()
                        );
                        log.info("Обновление старой лемы - " + oldLemma.getLemma() + " - со страницы " + page.getPath() + " в базе данных");
                        Index index = new Index();
                        index.setPageId(page);
                        index.setLemmaId(oldLemma);
                        index.setRank((float) e2);
                        indexes.add(index);
                    }
                });
                log.info("Добавление новых лемм в колличестве " + lemmas.size() + " со страницы " + page.getPath() + "  в базу данных");
                log.info("Поток: " + Thread.currentThread().getName());
                lemmaRepository.saveAll(lemmas);
                log.info("Новые леммы со страницы " + page.getPath() + " в колличестве " + lemmas.size() + " добавлены в базу данных\n");
                log.info("\nКоличество лемм в базе данных - " + lemmaRepository.count() + "\n");

                indexRepository.saveAll(indexes);
                log.info("Индексы со страницы " + page.getPath() + " в колличестве " + indexes.size() + " добавлены в базу данных");
                log.info("Колличество индексов в базе данных - " + indexRepository.count() + "\n");
                log.info("Колличество повторяющихся лем - " + countLemma);
            }
        }
    }
}
