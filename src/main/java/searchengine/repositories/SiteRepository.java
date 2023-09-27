package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel, Integer> {

    @Modifying
    @Query(value = "DELETE FROM site WHERE url = :url", nativeQuery = true)
    void clearData(String url);
}
