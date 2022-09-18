package main;

import java.util.concurrent.ForkJoinPool;

public class SearchEngineApplication {

    public static final String URL = "http://www.playback.ru/";

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        SiteNode siteNode = new SiteNode(URL);
        new ForkJoinPool().invoke(new SiteChildrenGetter(siteNode));
        DBConnection.executeMultiInsert();

        long total = (System.currentTimeMillis() - start) / 1000;
        System.out.println("Время выполнения: " + total + " секунд (или " + (total / 60.0) + " минут)");
    }
}
