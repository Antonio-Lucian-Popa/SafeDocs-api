package com.asusoftware.SafeDocs_api.repo;

import com.asusoftware.SafeDocs_api.domain.DocumentVersion;
import com.asusoftware.SafeDocs_api.dto.DocumentVersionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {
    List<DocumentVersion> findByDocumentIdOrderByVersionNoDesc(UUID documentId);

    @Query("select coalesce(max(v.versionNo),0) from DocumentVersion v where v.document.id = ?1")
    Integer findMaxVersionNoByDocumentId(UUID documentId);

    // repo
    @Query("""
  select new com.asusoftware.SafeDocs_api.dto.DocumentVersionItem(
    v.id, v.versionNo, v.filePath, v.mimeType, v.fileSize, v.createdAt
  )
  from DocumentVersion v
  join v.document d
  where d.id = :docId and d.user.id = :userId
  order by v.versionNo desc
""")
    List<DocumentVersionItem> findItemsForUserAndDocument(
            @org.springframework.data.repository.query.Param("userId") UUID userId,
            @org.springframework.data.repository.query.Param("docId") UUID docId
    );

}
