package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageModel, Integer> {

    @Modifying
    @Query(value = "DELETE p FROM page AS p JOIN site AS s ON p.site_id = s.id " +
            "WHERE url = :url", nativeQuery = true)
    void clearData(String url);

    @Query(value = "SELECT * FROM page AS p JOIN site AS s ON p.site_id = s.id " +
            "WHERE url = :url AND path = :path", nativeQuery = true)
    List<PageModel> findPage(String url, String path);

    @Modifying
    @Query(value = "DELETE p FROM page AS p JOIN site AS s ON p.site_id = s.id " +
            "WHERE url = :url AND path = :path", nativeQuery = true)
    void deletePage(String url, String path);
}
