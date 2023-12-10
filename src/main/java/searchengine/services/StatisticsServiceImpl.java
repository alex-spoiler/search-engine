package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sites;
    private final RepositoryService repos;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for(Site site : sites.getSites()) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            String url = site.getUrl();
            item.setName(site.getName());
            item.setUrl(url);

            if (repos.getSiteRepository().findSite(url).isEmpty()) {
                item.setStatus("—");
                item.setStatusTime((new Date()).getTime());
                item.setPages(0);
                item.setLemmas(0);
                item.setError("Индексация сайта еще не проводилась");
            } else {
                SiteModel siteModel = repos.getSiteRepository().findSite(url).get(0);
                int pages = repos.getPageRepository().getPagesAmount(url);
                int lemmas = repos.getLemmaRepository().getLemmasAmount(url);
                item.setPages(pages);
                item.setLemmas(lemmas);
                item.setStatus(siteModel.getStatus().toString());
                String error = siteModel.getLastError() == null ? "—" : siteModel.getLastError();
                item.setError(error);
                item.setStatusTime(siteModel.getStatusTime().getTime());
                total.setPages(total.getPages() + pages);
                total.setLemmas(total.getLemmas() + lemmas);
            }
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
