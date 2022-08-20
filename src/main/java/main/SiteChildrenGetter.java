package main;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class SiteChildrenGetter extends RecursiveTask<String> {
    private final SiteNode siteNode;
    private static int count = 0;

    public SiteChildrenGetter(SiteNode siteNode) {
        this.siteNode = siteNode;
    }

    @Override
    protected String compute() {
        count++;
        System.out.println("URL#" + count);
        StringBuilder links = new StringBuilder(siteNode.getUrl());
        List<SiteChildrenGetter> taskList = new LinkedList<>();

        for (SiteNode child : siteNode.getChildren()) {
            long timeSleep = 100 + (long) (Math.random() * 50);
            try {
                Thread.sleep(timeSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            SiteChildrenGetter task = new SiteChildrenGetter(child);
            task.fork();
            taskList.add(task);
        }

        for (SiteChildrenGetter task : taskList) {
            links.append("\n").append(task.join());
        }
        return links.toString();
    }
}
