package com.asusoftware.SafeDocs_api.repo;

import com.asusoftware.SafeDocs_api.domain.Document;
import com.asusoftware.SafeDocs_api.dto.DocumentListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
  select new com.asusoftware.SafeDocs_api.dto.DocumentListItem(
    d.id, d.title, d.folder.id, d.mimeType, d.fileSize, d.expiresAt, d.createdAt
  )
  from Document d
  where d.user.id = :uid
  order by d.createdAt desc
""")
    List<DocumentListItem> findTop50ForUser(@Param("uid") UUID uid, org.springframework.data.domain.Pageable pageable);

    @Query("""
  select new com.asusoftware.SafeDocs_api.dto.DocumentListItem(
    d.id, d.title, d.folder.id, d.mimeType, d.fileSize, d.expiresAt, d.createdAt
  )
  from Document d
  where d.user.id = :uid and lower(d.title) like lower(concat('%', :q, '%'))
  order by d.createdAt desc
""")
    List<DocumentListItem> searchByTitle(@Param("uid") UUID uid, @Param("q") String q);

    @Query(value = """
  select d.id, d.title, d.folder_id, d.mime_type, d.file_size, d.expires_at, d.created_at
  from documents d
  where d.user_id = :uid and coalesce(d.tags ->> :k, '') = :v
  order by d.created_at desc
  """, nativeQuery = true)
    List<Object[]> findByTagRaw(@Param("uid") UUID uid, @Param("k") String key, @Param("v") String val);

}
