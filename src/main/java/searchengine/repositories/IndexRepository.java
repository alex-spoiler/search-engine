package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexModel;

@Repository
public interface IndexRepository extends JpaRepository<IndexModel, Integer> {

    @Modifying
    @Query(value = "DELETE i FROM `index` AS i JOIN page AS p ON i.page_id = p.id " +
            "JOIN site AS s ON p.site_id = s.id WHERE url = :url", nativeQuery = true)
    void clearData(String url);

    @Modifying
    @Query(value = "DELETE i FROM `index` AS i JOIN page AS p ON i.page_id = p.id " +
            "JOIN site AS s ON p.site_id = s.id WHERE url = :url AND path = :path",
            nativeQuery = true)
    void deleteIndexesFromPage(String url, String path);
}
