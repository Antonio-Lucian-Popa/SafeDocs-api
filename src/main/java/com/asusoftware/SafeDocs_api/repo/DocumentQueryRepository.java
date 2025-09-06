package com.asusoftware.SafeDocs_api.repo;

import com.asusoftware.SafeDocs_api.domain.Document;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface DocumentQueryRepository extends Repository<Document, UUID> {

    @Query(value = """
      SELECT d.id as documentId, d.user_id as userId, d.title as title, d.expires_at as expiresAt,
             GREATEST(0, (d.expires_at::date - CURRENT_DATE))::int as daysLeft
      FROM documents d
      WHERE d.user_id = ?1
        AND d.expires_at IS NOT NULL
        AND d.expires_at::date >= CURRENT_DATE
        AND (d.expires_at::date - CURRENT_DATE) <= 30
      ORDER BY d.expires_at ASC
      """, nativeQuery = true)
    List<ExpiringSoonProjection> findExpiringSoon(UUID userId);
}