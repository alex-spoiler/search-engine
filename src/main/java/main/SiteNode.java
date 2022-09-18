package main;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
            Elements links = doc.select("a[href]");

            for (Element link : links) {
                String abs = link.attr("abs:href");

                if (isValidLink(abs)) {
                    SiteNode nodeChild = new SiteNode(abs);
                    nodeChildren.add(nodeChild);

                    PageContentGetter pageContent = new PageContentGetter(abs);
                    DBConnection.insertPage(link.attr("href"), pageContent.getResponseCode(), pageContent.getPageContent());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nodeChildren;
    }

    public boolean isValidLink(String abs) {
        if (abs.startsWith(url) && !abs.contains("#") && !(abs.endsWith(".jpg") || abs.endsWith(".jpeg") || abs.endsWith(".png")|| abs.endsWith(".gif"))) {
            return linkListGlobal.add(abs);
        }
        return false;
    }

    public static class PageContentGetter {
        private final StringBuilder pageContent = new StringBuilder();
        private final HttpURLConnection connection;
        private int responseCode;

        public PageContentGetter(String url) throws IOException {
            URL page = new URL(url);
            connection = (HttpURLConnection) page.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.connect();
        }

        public int getResponseCode() throws IOException {
            responseCode = connection.getResponseCode();
            return responseCode;
        }

        public String getPageContent() throws IOException {
            if (responseCode >= 200 && responseCode < 299) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.replace("'", "\\'");
                    pageContent.append(line);
                }
                br.close();
            } else {
                pageContent.append(" ");
            }
            return pageContent.toString();
        }
    }
}
