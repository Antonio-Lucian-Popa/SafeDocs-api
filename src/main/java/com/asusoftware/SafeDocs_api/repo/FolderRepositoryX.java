package com.asusoftware.SafeDocs_api.repo;

import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface FolderRepositoryX {

    @Query(value = """
      WITH RECURSIVE ancestors AS (
          SELECT f.id, f.parent_folder_id, f.user_id
          FROM folders f
          WHERE f.id = :folderId
        UNION ALL
          SELECT p.id, p.parent_folder_id, p.user_id
          FROM folders p
          JOIN ancestors a ON a.parent_folder_id = p.id
      )
      SELECT
        ( EXISTS (SELECT 1 FROM ancestors a WHERE a.user_id = :userId) ) OR
        ( EXISTS (
            SELECT 1 FROM folder_shares s
            WHERE s.shared_with_user_id = :userId
              AND s.folder_id IN (SELECT id FROM ancestors)
          ) )
      """, nativeQuery = true)
    boolean canReadFolderRecursive(UUID userId, UUID folderId);
}