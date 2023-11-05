package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaModel;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {

    @Query(value = "SELECT * FROM lemma AS l JOIN site AS s ON l.site_id = s.id " +
            "WHERE lemma = :lemma AND url = :url", nativeQuery = true)
    List<LemmaModel> findLemma(String lemma, String url);
}
