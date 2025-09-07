package com.asusoftware.SafeDocs_api.repo;

import com.asusoftware.SafeDocs_api.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByUserIdAndFolderIdOrderByTitleAsc(UUID userId, UUID folderId);
    List<Document> findByUserIdAndExpiresAtBetween(UUID userId, Instant from, Instant to);

    List<Document> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Document> findByUserIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(UUID userId, String title);

    @Query(value = """
        SELECT d FROM Document d
        WHERE d.user.id = :uid
          AND FUNCTION('jsonb_extract_path_text', d.tags, :k) = :v
        ORDER BY d.createdAt DESC
    """)
    List<Document> findByUserIdAndTag(UUID uid, String k, String v);
}
