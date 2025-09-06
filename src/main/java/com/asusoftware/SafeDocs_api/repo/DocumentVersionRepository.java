package com.asusoftware.SafeDocs_api.repo;

import com.asusoftware.SafeDocs_api.domain.DocumentVersion;
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
}
