package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Modifying
    @Query(value = "DELETE FROM `index` a WHERE a.page_id =:page_id", nativeQuery = true)
    void deleteWherePageId(@Param("page_id") Integer pageId);

    @Modifying
    @Query(value = "SELECT * FROM `index` WHERE `lemma_id` =?", nativeQuery = true)
    List<Index> findWhereLemmaId(@Param("lemma_id") Integer id);


    @Query(value = "SELECT * FROM `index` WHERE `lemma_id`=? AND page_id=?", nativeQuery = true)
    Index findWhereLemmaIdPageId(@Param("lemma_id, page_id") Integer lemmaId, Integer pageId);

}
