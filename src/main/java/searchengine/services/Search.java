package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@Service
public class Search {
    private final LemmaSearch lemmaSearch;
    private final SearchData data;
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    @Value("${app.frequency percentage}")
    private int FREQUENCY_PERCENTAGE;
    private SiteEntity siteEntity;
    private int count = 0;
    List<Integer> indexBeforeList = new ArrayList<>();
    List<Integer> indexAfterList = new ArrayList<>();

    public Search(LemmaSearch lemmaSearch, SitesList sitesList, SearchData data, SiteRepository siteRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, PageRepository pageRepository) {
        this.lemmaSearch = lemmaSearch;
        this.data = data;
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
    }

    public SearchResponse getResponse(String query, String site, int offset, int limit) {
        try {
            int countRelevance = 0;
            SearchResponse response = new SearchResponse();
            response.setResult(true);
            if (query.isEmpty()) {
                response.setResult(false);
                response.setError("Задан пустой поисковый запрос");
                log.error("Задан пустой поисковый запрос");
                return response;
            }
            if (site != null) {
                siteEntity = siteRepository.findByUrl(site).orElseThrow();
            } else {
                log.info("Поиск по всем сайтам");
            }
            List<Lemma> lemmaList = getLemmas(query);
            List<Lemma> lemmaForMap = new ArrayList<>();
            Map<SiteEntity, List<Lemma>> map = lemmaList.stream().collect(Collectors.groupingBy(Lemma::getSiteId));
            List<Page> pagesFirstLemma = new ArrayList<>();
            if (site != null) {
                List<Lemma> listMap = map.get(siteEntity);
                if (listMap != null) {
                    lemmaForMap.addAll(listMap);
                    log.info("Колличество страниц в списке после удаления: " +
                            getPages(listMap, pagesFirstLemma, query).size());
                }
            } else {
                map.forEach((l1, l2) -> {
                    List<Page> pageList = getPages(l2, pagesFirstLemma, query);
                    lemmaForMap.addAll(l2);
                    log.info("Колличество страниц в списке после удаления: " + pageList.size());
                });
            }
            log.info(" Список страниц по первой лемме: ");
            pagesFirstLemma.forEach(page -> log.info(page.getId() + "/" + page.getSite().getUrl().substring(0, page.getSite().getUrl().length() - 1) + page.getPath()));
            List<SearchData> dataList = new ArrayList<>();
            response.setCount(pagesFirstLemma.size());
            List<Float> relevance = relevant(lemmaForMap, pagesFirstLemma);
            for (Page page : pagesFirstLemma) {
                countRelevance++;
                Document doc = Jsoup.parse(page.getContent());
                SearchData searchData = new SearchData();
                searchData.setUri(page.getPath());
                searchData.setSiteName(page.getSite().getName());
                searchData.setSite(page.getSite().getUrl().substring(0, page.getSite().getUrl().length() - 1));
                searchData.setTitle(doc.title());
                searchData.setSnippet(getSnippet(page, query));
                searchData.setRelevance(relevance.get(countRelevance - 1));
                dataList.add(searchData);
            }
            dataList.sort(Comparator.comparing(SearchData::getRelevance).reversed());
            List<SearchData> dataListFinal = new ArrayList<>();
            if (dataList.size() > limit) {
                for (int i = 0; i < dataList.size(); i++) {
                    if (i == limit) {
                        int size = Math.min(offset + limit, dataList.size());
                        dataListFinal.addAll(dataList.subList(offset, size));
                    }
                }
                response.setData(dataListFinal);
            } else {
                response.setData(dataList);
            }
            if (dataList.isEmpty()) {
                response.setData(Collections.emptyList());
            }
            return response;
        } catch (Exception e) {
            log.error(e.getMessage());
            return new SearchResponse(false,e.getMessage());
        }
    }

    public List<Lemma> getLemmas(String query) {
        Set<String> lemmasQuery = lemmaSearch.getLemmas(query).keySet();
        List<Lemma> lemmaList = new ArrayList<>();
        lemmasQuery.forEach(lemmaWord -> {
            List<Lemma> lemmaListDB = lemmaRepository.findByLemma(lemmaWord);
            lemmaListDB.forEach(lemma -> {
                int countPage = pageRepository.countWhereSiteId(lemma.getSiteId().getId());
                double frequent = ((double) lemma.getFrequency() / countPage) * 100;
                if (frequent < FREQUENCY_PERCENTAGE) {
                    lemmaList.add(lemma);
                }
            });
        });
        return lemmaList;
    }

    public List<Page> getPages(List<Lemma> listMap, List<Page> remainingPages, String query) {
        count = 0;
        List<Page> pages = new ArrayList<>();
        if (listMap.size() != lemmaSearch.getLemmas(query).size()) {
            return pages;
        }
        listMap.sort(Comparator.comparing(Lemma::getFrequency));
        List<Index> indexList = indexRepository.findWhereLemmaId(listMap.get(0).getId());
        log.info("Первая самая редкая лемма - " + listMap.get(0).getId() + "/" + listMap.get(0).getLemma() + "/" + listMap.get(0).getFrequency() + "/" + listMap.get(0).getSiteId().getName());
        indexList.forEach(index -> {
            pages.add(index.getPageId());
            remainingPages.add(index.getPageId());
        });
        log.info("Колличество страниц в списке до удаления: " + remainingPages.size());
        for (int i = 1; i < listMap.size(); i++) {
            for (Page page : pages) {
                Index index = indexRepository.findWhereLemmaIdPageId(listMap.get(i).getId(), page.getId());
                if (index == null) {
                    log.info("Удалена страница - " + page.getId() + "/с сайта: " + page.getSite().getName());
                    remainingPages.remove(page);
                }
            }
        }
        return remainingPages;
    }

    public String getSnippet(Page page, String query) {
        Document doc = Jsoup.parse(page.getContent());
        String text = doc.text();
        Set<String> lemmasQuery = lemmaSearch.getLemmas(query).keySet();
        String[] wordsText = text.split("\\s+");
        List<Integer> indexes = new ArrayList<>();
        count++;
        log.info("Страница - " + count + " /" + page.getId() + "/" + page.getSite().getName());
        List<String> listWords = new LinkedList<>(Arrays.asList(wordsText));
        for (int i = 0; i < wordsText.length; i++) {
            for (String lemmaQuery : lemmasQuery) {
                Set<String> lemmas = lemmaSearch.getLemmas(wordsText[i]).keySet();
                List<String> lemmaList = new ArrayList<>(lemmas);
                if (lemmaList.isEmpty()) {
                    continue;
                }
                String lemmaWord = lemmaList.get(0);
                if (lemmaWord.equalsIgnoreCase(lemmaQuery)) {
                    indexes.add(i);
                }
            }
        }
        return addTag(indexes, listWords);
    }

    public String addTag(List<Integer> indexes, List<String> list) {
        StringBuilder snippetAddTag = new StringBuilder();
        String tagBefore = "<b>";
        String tagAfter = "</b>";
        for (int i = 0; i < indexes.size(); i++) {
            int index = indexes.get(i) + i * 2;
            if (index < list.size()) {
                list.add(index, tagBefore);
                if(index + 2 < list.size()) {
                    list.add(index + 2, tagAfter);
                }
            }
        }
        for (String word : list) {
            snippetAddTag.append(word).append(" ");
        }
        return getLine(snippetAddTag.toString());
    }

    public List<Float> relevant(List<Lemma> lemmas, List<Page> pages) {
        List<Float> absoluteRelevant = new ArrayList<>();
        List<Float> relativeRelevance = new ArrayList<>();
        for (Page page : pages) {
            float absRelevance = 0;
            for (Lemma lemma : lemmas) {
                Index index = indexRepository.findWhereLemmaIdPageId(lemma.getId(), page.getId());
                if (index != null) {
                    absRelevance += index.getRank();
                }
            }
            absoluteRelevant.add(absRelevance);
        }
        if (!absoluteRelevant.isEmpty()) {
            Float max = absoluteRelevant.stream().max(Float::compareTo).orElseThrow();
            for (Float relevance : absoluteRelevant) {
                float relevant = relevance / max;
                relativeRelevance.add(relevant);
            }
        }
        return relativeRelevance;
    }

    public String getLine(String snippet) {
        String[] words = snippet.split("\\s+");
        StringBuilder line = new StringBuilder();
        StringBuilder finalSnippet = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0 && words[i].equals("<b>") && words[i - 1].equals("</b>")) {
                continue;
            }
            if (i < words.length - 3) {
                if (words[i].equals("</b>") && words[i + 3].equals("</b>")) {
                    continue;
                }
            }
            line.append(words[i]).append(" ");
        }
        List<Integer> indicesTagOne = wordIndices(line.toString(), "<b>");
        List<Integer> indicesTagTwo = wordIndices(line.toString(), "</b>");
        addIndexBeforeListAndIndexAfterList(indicesTagOne, indicesTagTwo, line.toString());
        for (int i = 0; i < indicesTagOne.size(); i++) {
            int before = indexBeforeList.get(i);
            int after = indexAfterList.get(i);
            String fragmentSnippet = trimLine(line.toString(), before, after);
            if (before != 0) {
                fragmentSnippet = " ... " + fragmentSnippet;
            }
            if (after != line.length()) {
                fragmentSnippet = fragmentSnippet + " ... ";
            }
            finalSnippet.append(fragmentSnippet).append(" ");
        }
        return finalSnippet.toString();
    }

    public void addIndexBeforeListAndIndexAfterList(List<Integer> indices, List<Integer> indicesTwo, String line) {
        indexBeforeList.clear();
        indexAfterList.clear();
        for (int i = 0; i < indices.size(); i++) {
            int before = Math.max(indices.get(i) - 20, 0);
            int after = Math.min(indicesTwo.get(i) + 30, line.length() -1);
            indexBeforeList.add(before);
            indexAfterList.add(after);
        }
        for (int i = 0; i < indexAfterList.size(); i++) {
            for (int j = i + 1; j < indexBeforeList.size(); j++) {
                if (indexAfterList.get(i) > indices.get(j)) {
                    indexAfterList.set(i, indices.get(j) - 1);
                }
                if (indicesTwo.get(i) > indexBeforeList.get(j)) {
                    indexBeforeList.set(j, indicesTwo.get(i));
                }
            }
        }
    }

    public List<Integer> wordIndices(String line, String word) {
        List<Integer> indices = new ArrayList<>();
        int wordLength = word.length();
        int index = line.indexOf(word);
        while (index >= 0) {
            indices.add(index);
            index = line.indexOf(word, index + wordLength);
        }
        return indices;
    }

    public String trimLine(String line, int numberBefore, int numberAfter) {
        String substring = line.substring(numberBefore, numberAfter);
        if (line.charAt(numberAfter) != ' ' && !Character.isWhitespace(line.charAt(numberAfter))) {
            int lastSpaceIndex = substring.lastIndexOf(' ');
            if (lastSpaceIndex != -1) {
                substring = substring.substring(0, lastSpaceIndex);
            }
        }
        if (substring.charAt(0) != ' ' && !Character.isWhitespace(substring.charAt(0))) {
            int firstSpaceIndex = substring.indexOf(' ');
            if (firstSpaceIndex != -1) {
                substring = substring.substring(firstSpaceIndex + 1);
            }
        }
        return substring;
    }
}


