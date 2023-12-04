package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.UserAgent;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class PageData {
    private static final Pattern PATTERN_FILE = Pattern.compile("([^\\s]+(\\.(?i)(jpg|jpeg|png|gif|bmp|pdf))$)");
    private static final Pattern PATTERN_ANCHOR = Pattern.compile("#([\\w\\-]+)?$");

    private final SiteModel siteModel;
    private final String pageLink;
    private final RepositoryService repos;
    private final UserAgent userAgent;
    private final String mainPage;
    private final Set<PageData> pageChildren;

    private Document document;
    private int responseCode;
    private boolean isAvailable;

    public PageData(SiteModel siteModel, String pageLink, RepositoryService repos, UserAgent userAgent) {
        this.siteModel = siteModel;
        this.pageLink = pageLink;
        this.repos = repos;
        this.userAgent = userAgent;
        mainPage = siteModel.getUrl();
        pageChildren = new HashSet<>();
    }

    public boolean connecting() {
        try {
            Connection.Response response = Jsoup.connect(pageLink)
                    .userAgent(userAgent.getUserAgent()).referrer(userAgent.getReferrer())
                    .ignoreHttpErrors(true).ignoreContentType(true).execute();
            responseCode = response.statusCode();
            document = response.parse();
            isAvailable = true;
        } catch (IOException e) {
            System.out.printf("%s: Connection error: %s\n", this.getClass().getSimpleName(), e.getMessage());
            siteModel.setStatus(SiteStatus.FAILED);
            siteModel.setLastError("Сайт не доступен");
            isAvailable = false;
        } finally {
            siteModel.setStatusTime(new Date());
            repos.getSiteRepository().saveAndFlush(siteModel);
        }
        return isAvailable;
    }

    public void pageIndexing() {
        if (isAvailable) {
            repos.getPageRepository().saveAndFlush(getPageModel());

            if (responseCode < 400) {
                HashMap<String, Integer> lemmas = LemmaFinder.getInstance().getLemmasCollection(document.text());
                writeLemmas(lemmas);
                writeIndexes(lemmas);
            }
        } else {
            System.out.printf("%s: Connection error", this.getClass().getSimpleName());
        }
    }

    public Collection<PageData> getChildren() {
        if (document == null) {
            return Collections.emptyList();
        }

        Elements links = document.select("a[href]");
        for (Element link : links) {
            String absLink = link.attr("abs:href");
            if (isValidLink(absLink)) {
                PageData pageChild = new PageData(siteModel, absLink, repos, userAgent);
                if (pageChild.connecting()) {
                    pageChild.pageIndexing();
                }
                pageChildren.add(pageChild);
            }
        }
        return pageChildren;
    }

    private boolean isValidLink(String absLink) {
        if (absLink.startsWith(pageLink) && !PATTERN_FILE.matcher(absLink).find()
                && !PATTERN_ANCHOR.matcher(absLink).find()) {
            return repos.getPageRepository().findPage(mainPage, getRelLink(absLink)).isEmpty();
        }
        return false;
    }

    private String getRelLink(String absLink) {
        return absLink.equals(mainPage) ? "/" : absLink.substring(mainPage.length());
    }

    private PageModel getPageModel() {
        PageModel pageModel = new PageModel();
        pageModel.setSite(siteModel);
        pageModel.setPath(getRelLink(pageLink));
        pageModel.setCode(responseCode);
        pageModel.setContent(document.toString());
        return pageModel;
    }

    private void writeLemmas(HashMap<String, Integer> lemmas) {
        for (String lemma : lemmas.keySet()) {
            LemmaModel lemmaModel;
            if (repos.getLemmaRepository().findLemma(lemma, siteModel.getUrl()).isEmpty()) {
                lemmaModel = new LemmaModel();
                lemmaModel.setLemma(lemma);
                lemmaModel.setSite(siteModel);
                lemmaModel.setFrequency(1);
            } else {
                lemmaModel = repos.getLemmaRepository().findLemma(lemma, siteModel.getUrl()).get(0);
                lemmaModel.setFrequency(lemmaModel.getFrequency() + 1);
            }

            repos.getLemmaRepository().saveAndFlush(lemmaModel);
        }
    }

    private void writeIndexes(HashMap<String, Integer> lemmas) {
        for (String lemma : lemmas.keySet()) {
            IndexModel indexModel = new IndexModel();
            indexModel.setPage(repos.getPageRepository().findPage(mainPage, getRelLink(pageLink)).get(0));
            indexModel.setLemma(repos.getLemmaRepository().findLemma(lemma, siteModel.getUrl()).get(0));
            indexModel.setRank(lemmas.get(lemma));
            repos.getIndexRepository().saveAndFlush(indexModel);
        }
    }
}
