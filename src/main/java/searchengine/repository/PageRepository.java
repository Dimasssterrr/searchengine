package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.List;

@Repository

public interface PageRepository extends JpaRepository<Page,Integer> {
    Page findByPath(String path);

    @Query(value = "SELECT COUNT(*) FROM page WHERE site_id =?",nativeQuery = true)
    Integer countWhereSiteId(@Param("site_id") Integer siteId);

    @Modifying
    @Query(value = "SELECT * FROM `page` a WHERE  site_id =:site_id", nativeQuery = true)
    List<Page> findWhereSiteId(@Param("site_id") Integer siteId);
    @Modifying
    @Query(value = "SELECT * FROM `page` p INNER JOIN `index` i ON p.id=i.page_id WHERE i.lemma_id =?",nativeQuery = true)
    List<Page> findWhereLemmaId(@Param("lemma_id") Integer lemmaId);
    @Modifying
    @Query(value = "DELETE FROM `page` a WHERE a.site_id =:site_id", nativeQuery = true)
    void deleteWhereSiteId(@Param("site_id") Integer siteId);

    void deleteByPath(String path);
}
