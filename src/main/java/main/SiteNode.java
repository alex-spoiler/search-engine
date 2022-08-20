package main;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

public class SiteNode {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
    private static final String REFERRER = "http://www.google.com";

    private final String url;
    private final Set<SiteNode> nodeChildren;
    private static final Set<String> linkListGlobal = new LinkedHashSet<>();

    public SiteNode(String url) {
        this.url = url;
        nodeChildren = new LinkedHashSet<>();
    }

    public String getUrl() {
        return url;
    }

    public Collection<SiteNode> getChildren() {
        try {
            Document doc = Jsoup.connect(url).userAgent(USER_AGENT).referrer(REFERRER).get();
            Elements links = doc.select("a");

            for (Element link : links) {
                String abs = link.attr("abs:href");
                if (abs.startsWith(url) && !abs.endsWith("#") && !abs.endsWith(".jpg")) {
                    boolean isRepeat = !linkListGlobal.add(abs);

                    if (!isRepeat) {
                        SiteNode nodeChild = new SiteNode(abs);
                        nodeChildren.add(nodeChild);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nodeChildren;
    }
}
