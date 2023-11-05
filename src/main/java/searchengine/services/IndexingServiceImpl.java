package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.UserAgent;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{
    private final SitesList sites;
    private final RepositoryService repos;
    private final UserAgent userAgent;

    private ExecutorService executorService;
    private ForkJoinPool forkJoinPool;
    private List<SiteModel> siteModelList;

    @Override
    @Transactional
    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse();

        if (executorService == null || executorService.isTerminated()) {
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            forkJoinPool = new ForkJoinPool();
        }

        if (executorService.isShutdown()) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }

        siteModelList = new ArrayList<>();
        for (Site site : sites.getSites()) {
            String url = site.getUrl();

            repos.getPageRepository().clearData(url);
            repos.getSiteRepository().clearData(url);

            SiteModel siteModel = new SiteModel();
            siteModel.setUrl(url);
            siteModel.setName(site.getName());
            siteModel.setStatus(SiteStatus.INDEXING);
            siteModel.setStatusTime(new Date());
            repos.getSiteRepository().saveAndFlush(siteModel);
            siteModelList.add(siteModel);

            PageData pageData = new PageData(siteModel, url, repos, userAgent);
            Runnable siteIndexing = () -> {
                if (pageData.connecting()) {
                    pageData.pageIndexing();
                    forkJoinPool.invoke(new PageDataRecursiveTask(pageData));
                }

                if (siteModel.getStatus() != SiteStatus.FAILED) {
                    siteModel.setStatus(SiteStatus.INDEXED);
                    repos.getSiteRepository().saveAndFlush(siteModel);
                }
            };
            executorService.execute(siteIndexing);
        }
        executorService.shutdown();

        response.setResult(true);
        response.setError("");
        return response;
    }

    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse response = new IndexingResponse();

        if (executorService == null || executorService.isTerminated()) {
            response.setResult(false);
            response.setError("Индексация не запущена");
            return response;
        }

        forkJoinPool.shutdownNow();

        for (SiteModel siteModel : siteModelList) {
            if (siteModel.getStatus() == SiteStatus.INDEXING) {
                siteModel.setStatus(SiteStatus.FAILED);
                siteModel.setLastError("Индексация остановлена пользователем");
                repos.getSiteRepository().saveAndFlush(siteModel);
            }
        }

        response.setResult(true);
        response.setError("");
        return response;
    }

    @Override
    @Transactional
    public IndexingResponse indexPage(String url) {
        IndexingResponse response = new IndexingResponse();

        if (url.isBlank()) {
            response.setResult(false);
            response.setError("Укажите страницу сайта, которую нужно обновить или добавить");
            return response;
        }

        if (!isCorrectPage(url)) {
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return response;
        }

        SiteModel siteModel = getSiteModel(url);
        String path = url.substring(siteModel.getUrl().length());
        PageData pageData = new PageData(siteModel, url, repos, userAgent);

        if (!pageData.connecting()) {
            response.setResult(false);
            response.setError("Данная страница не доступна");
            return response;
        }

        if (!repos.getPageRepository().findPage(siteModel.getUrl(), path).isEmpty()) {
            repos.getPageRepository().deletePage(siteModel.getUrl(), path);
            //todo: +delete index
            //todo: +delete lemma
        }

        pageData.pageIndexing();

        response.setResult(true);
        response.setError("");
        return response;
    }

    private SiteModel getSiteModel(String url) {
        SiteModel siteModel = new SiteModel();
        String mainPage = null;
        String name = null;
        for (Site site : sites.getSites()) {
            if (url.startsWith(site.getUrl())) {
                mainPage = site.getUrl();
                name = site.getName();
                break;
            }
        }

        if (repos.getSiteRepository().findSite(mainPage).isEmpty()) {
            siteModel.setUrl(mainPage);
            siteModel.setName(name);
            siteModel.setStatus(SiteStatus.INDEXING);
            siteModel.setStatusTime(new Date());
            repos.getSiteRepository().saveAndFlush(siteModel);
        } else {
            siteModel = repos.getSiteRepository().findSite(mainPage).get(0);
        }
        return siteModel;
    }

    private boolean isCorrectPage(String url) {
        for (Site site : sites.getSites()) {
            if (url.startsWith(site.getUrl())) {
                return true;
            }
        }
        return false;
    }
}
