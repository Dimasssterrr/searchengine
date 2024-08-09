package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
@Transactional
public interface SiteRepository extends JpaRepository<SiteEntity,Integer> {

    Optional<SiteEntity> findByUrl(String url);
    SiteEntity findByName(String name);

    void deleteByUrl(String url);

}
