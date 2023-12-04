package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaModel;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {

    @Modifying
    @Query(value = "DELETE l FROM lemma AS l JOIN site AS s ON l.site_id = s.id " +
            "WHERE url = :url", nativeQuery = true)
    void clearData(String url);

    @Query(value = "SELECT * FROM lemma AS l JOIN site AS s ON l.site_id = s.id " +
            "WHERE lemma = :lemma AND url = :url", nativeQuery = true)
    List<LemmaModel> findLemma(String lemma, String url);

    @Query(value = "SELECT * FROM lemma AS l JOIN `index` AS i ON l.id = i.lemma_id " +
            "JOIN page AS p ON p.id = i.page_id JOIN site AS s ON l.site_id = s.id " +
            "WHERE url = :url AND path = :path", nativeQuery = true)
    List<LemmaModel> getLemmasFromPage(String url, String path);
}
