package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
   Lemma findByLemmaAndSiteId(String lemma, SiteEntity site);
   @Query(value = "SELECT * FROM lemma L INNER JOIN `index` I ON L.id=I.lemma_id WHERE page_id=?", nativeQuery = true)
   List<Lemma> findAllWherePageId(@Param("page_id") Integer id);

   List<Lemma> findByLemma(String lemma);
   @Modifying
   @Query(value = "DELETE FROM `lemma` a WHERE a.site_id =:site_id", nativeQuery = true)
   void deleteWherePageId(@Param("site_id") Integer siteId);

   @Query(value = "SELECT COUNT(*) FROM `lemma` WHERE site_id=?", nativeQuery = true)
   Integer countWhereSiteId(@Param("site_id") Integer site_id);

}
