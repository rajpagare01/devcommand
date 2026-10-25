package com.devcommand.devcommand.dsa.catalog.repository;

import com.devcommand.devcommand.dsa.catalog.entity.DsaProblemCatalogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DsaProblemCatalogRepository extends JpaRepository<DsaProblemCatalogEntry, Long> {

    Optional<DsaProblemCatalogEntry> findByNormalizedTitle(String normalizedTitle);

    Optional<DsaProblemCatalogEntry> findByTitleIgnoreCase(String title);

    @Query("SELECT e FROM DsaProblemCatalogEntry e WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR e.normalizedTitle LIKE LOWER(CONCAT('%', :query, '%'))")
    List<DsaProblemCatalogEntry> searchSimilar(@Param("query") String query);
}
