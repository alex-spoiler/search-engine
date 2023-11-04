package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel, Integer> {

    @Modifying
    @Query(value = "DELETE FROM site WHERE url = :url", nativeQuery = true)
    void clearData(String url);

    @Query(value = "SELECT * FROM site WHERE url = :url", nativeQuery = true)
    List<SiteModel> findSite(String url);
}
