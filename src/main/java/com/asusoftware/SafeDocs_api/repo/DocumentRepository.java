package com.asusoftware.SafeDocs_api.repo;

import com.asusoftware.SafeDocs_api.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByUserIdAndFolderIdOrderByTitleAsc(UUID userId, UUID folderId);
    List<Document> findByUserIdAndExpiresAtBetween(UUID userId, Instant from, Instant to);
}
