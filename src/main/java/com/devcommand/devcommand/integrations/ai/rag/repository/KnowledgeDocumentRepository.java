package com.devcommand.devcommand.integrations.ai.rag.repository;

import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeDocument;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    @Query("SELECT k FROM KnowledgeDocument k WHERE k.documentId = :documentId AND k.owner.id = :ownerId")
    Optional<KnowledgeDocument> findByDocumentIdAndOwnerId(@Param("documentId") String documentId, @Param("ownerId") Long ownerId);

    @Query("SELECT k FROM KnowledgeDocument k WHERE k.documentId = :documentId AND k.owner IS NULL")
    Optional<KnowledgeDocument> findByDocumentIdAndOwnerIsNull(@Param("documentId") String documentId);

    @Query("SELECT k FROM KnowledgeDocument k WHERE k.owner.id = :ownerId")
    Page<KnowledgeDocument> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    Page<KnowledgeDocument> findByVisibility(KnowledgeVisibility visibility, Pageable pageable);

    @Query("SELECT k FROM KnowledgeDocument k WHERE k.documentId = :documentId AND (k.owner.id = :userId OR k.visibility = 'SYSTEM' OR k.visibility = 'SHARED')")
    Optional<KnowledgeDocument> findAccessibleDocument(@Param("documentId") String documentId, @Param("userId") Long userId);

    void deleteByDocumentIdAndOwnerId(String documentId, Long ownerId);

    void deleteByDocumentIdAndOwnerIsNull(String documentId);
}
