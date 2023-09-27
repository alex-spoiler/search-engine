package searchengine.services;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

public class PageDataRecursiveTask extends RecursiveAction {
    private final PageData pageData;

    public PageDataRecursiveTask(PageData pageData) {
        this.pageData = pageData;
    }

    @Override
    protected void compute() {
        List<PageDataRecursiveTask> taskList = new LinkedList<>();

        for (PageData pageChild : pageData.getChildren()) {
            long timeSleep = 500 + (long) (Math.random() * 1000);
            try {
                Thread.sleep(timeSleep);
            } catch (InterruptedException e) {
                System.out.printf("%s: Thread message: %s\n", this.getClass().getSimpleName(), e.getMessage());
                break;
            }

            PageDataRecursiveTask task = new PageDataRecursiveTask(pageChild);
            task.fork();
            taskList.add(task);
        }

        for (PageDataRecursiveTask task : taskList) {
            task.join();
        }
    }
}
