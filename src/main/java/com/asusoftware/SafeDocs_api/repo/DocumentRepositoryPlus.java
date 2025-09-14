package com.asusoftware.SafeDocs_api.repo;

import com.asusoftware.SafeDocs_api.domain.Document;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface DocumentRepositoryPlus {

    @Query(value = """
      WITH RECURSIVE subfolders AS (
          SELECT f.id
          FROM folders f
          WHERE f.id = :folderId
        UNION ALL
          SELECT c.id
          FROM folders c
          JOIN subfolders p ON c.parent_folder_id = p.id
      ),
      my_access AS (
          SELECT :userId::uuid AS uid
      )
      SELECT d.*
      FROM documents d
      LEFT JOIN subfolders sf ON sf.id = d.folder_id
      JOIN folders f ON f.id = COALESCE(d.folder_id, f.id) -- doar ca să avem ownerul
      WHERE
           -- Owner:
           f.user_id = :userId
        OR
           -- Shared direct pe folderul cerut sau pe un părinte:
           EXISTS (
             SELECT 1 FROM folder_shares s
             WHERE s.folder_id IN (SELECT id FROM subfolders)
               AND s.shared_with_user_id = :userId
           )
      ORDER BY d.created_at DESC
      """, nativeQuery = true)
    List<Document> listDocsWithShareForFolder(
            @Param("userId") UUID userId,
            @Param("folderId") UUID folderId
    );
}
