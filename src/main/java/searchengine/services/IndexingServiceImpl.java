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
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

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
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
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

            pageRepository.clearData(url);
            siteRepository.clearData(url);

            SiteModel siteModel = new SiteModel();
            siteModel.setUrl(url);
            siteModel.setName(site.getName());
            siteModel.setStatus(SiteStatus.INDEXING);
            siteModel.setStatusTime(new Date());
            siteRepository.saveAndFlush(siteModel);
            siteModelList.add(siteModel);

            PageData pageData = new PageData(siteModel, url, siteRepository, pageRepository, userAgent);
            Runnable siteIndexing = () -> {
                if (pageData.pageIndexing()) {
                    forkJoinPool.invoke(new PageDataRecursiveTask(pageData));
                }

                if (siteModel.getStatus() == SiteStatus.INDEXING) {
                    siteModel.setStatus(SiteStatus.INDEXED);
                    siteRepository.saveAndFlush(siteModel);
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
                siteRepository.saveAndFlush(siteModel);
            }
        }

        response.setResult(true);
        response.setError("");
        return response;
    }
}
