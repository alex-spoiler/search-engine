package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.UserAgent;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class PageData {
    private static final Pattern PATTERN_FILE = Pattern.compile("([^\\s]+(\\.(?i)(jpg|jpeg|png|gif|bmp|pdf))$)");
    private static final Pattern PATTERN_ANCHOR = Pattern.compile("#([\\w\\-]+)?$");

    private final SiteModel siteModel;
    private final String pageLink;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final UserAgent userAgent;
    private final String mainPage;
    private final Set<PageData> pageChildren;

    private Document document;

    public PageData(SiteModel siteModel, String pageLink, SiteRepository siteRepository,
                    PageRepository pageRepository, UserAgent userAgent) {
        this.siteModel = siteModel;
        this.pageLink = pageLink;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.userAgent = userAgent;
        mainPage = siteModel.getUrl();
        pageChildren = new HashSet<>();
    }

    public boolean pageIndexing() {
        boolean isAvailable;
        try {
            Connection.Response response = Jsoup.connect(pageLink)
                    .userAgent(userAgent.getUserAgent()).referrer(userAgent.getReferrer())
                    .ignoreHttpErrors(true).ignoreContentType(true).execute();
            int responseCode = response.statusCode();
            document = response.parse();

            PageModel pageModel = new PageModel();
            pageModel.setSite(siteModel);
            pageModel.setPath(getRelLink(pageLink));
            pageModel.setCode(responseCode);
            pageModel.setContent(document.toString());
            pageRepository.saveAndFlush(pageModel);
            isAvailable = true;
        } catch (IOException e) {
            System.out.printf("%s: Connection error: %s\n", this.getClass().getSimpleName(), e.getMessage());
            siteModel.setStatus(SiteStatus.FAILED);
            siteModel.setLastError("Сайт не доступен");
            isAvailable = false;
        } finally {
            siteModel.setStatusTime(new Date());
            siteRepository.saveAndFlush(siteModel);
        }
        return isAvailable;
    }

    public Collection<PageData> getChildren() {
        if (document == null) {
            return Collections.emptyList();
        }

        Elements links = document.select("a[href]");
        for (Element link : links) {
            String absLink = link.attr("abs:href");
            if (isValidLink(absLink)) {
                PageData pageChild = new PageData(siteModel, absLink,
                        siteRepository, pageRepository, userAgent);
                pageChild.pageIndexing();
                pageChildren.add(pageChild);
            }
        }
        return pageChildren;
    }

    private boolean isValidLink(String absLink) {
        if (absLink.startsWith(pageLink) && !PATTERN_FILE.matcher(absLink).find()
                && !PATTERN_ANCHOR.matcher(absLink).find()) {
            return pageRepository.findPage(mainPage, getRelLink(absLink)).isEmpty();
        }
        return false;
    }

    private String getRelLink(String absLink) {
        return absLink.equals(mainPage) ? "/" : absLink.substring(mainPage.length());
    }
}
