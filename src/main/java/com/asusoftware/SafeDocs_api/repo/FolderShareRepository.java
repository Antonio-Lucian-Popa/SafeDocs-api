package com.asusoftware.SafeDocs_api.repo;

import com.asusoftware.SafeDocs_api.domain.Folder;
import com.asusoftware.SafeDocs_api.domain.FolderShare;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface FolderShareRepository extends JpaRepository<FolderShare, UUID> {

    @Query("""
      select fs from FolderShare fs
      join fetch fs.folder f
      join fetch fs.sharedWith u
      where f.id = :folderId
      order by u.email asc
    """)
    List<FolderShare> listSharesForFolder(@Param("folderId") UUID folderId);

    @Query("""
      select (count(fs) > 0)
      from FolderShare fs
      where fs.folder.id = :folderId
        and fs.sharedWith.id = :userId
        and (fs.permission = 'READ' or fs.permission = 'WRITE')
    """)
    boolean hasAnyAccess(@Param("userId") UUID userId, @Param("folderId") UUID folderId);

    @Query("""
      select (count(fs) > 0)
      from FolderShare fs
      where fs.folder.id = :folderId
        and fs.sharedWith.id = :userId
        and fs.permission = 'WRITE'
    """)
    boolean hasWriteAccess(@Param("userId") UUID userId, @Param("folderId") UUID folderId);

    // exists / find / delete by IDs (nume CONSISTENTE cu ce folosim în controller)
    boolean existsByFolder_IdAndSharedWith_Id(UUID folderId, UUID userId);
    Optional<FolderShare> findByFolder_IdAndSharedWith_Id(UUID folderId, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
    delete from FolderShare fs
    where fs.folder.id = :folderId
      and fs.sharedWith.id = :userId
  """)
    int deleteShare(@Param("folderId") UUID folderId, @Param("userId") UUID userId);

    // opțional: căutare după email (pentru upsert rapid la share)
    @Query("""
      select fs from FolderShare fs
      where fs.folder.id = :folderId and lower(fs.sharedWith.email) = lower(:email)
    """)
    List<FolderShare> findByFolderIdAndSharedWithEmail(@Param("folderId") UUID folderId,
                                                       @Param("email") String email);

    // opțional: listă simplă fără fetch join
    List<FolderShare> findByFolder_Id(UUID folderId);

    @Query("""
  select fs.folder
  from FolderShare fs
  where fs.sharedWith.id = :uid
  order by fs.folder.name asc
""")
    List<Folder> findSharedFoldersForUser(@Param("uid") UUID userId);

    @Query("""
      select (count(fs) > 0)
      from FolderShare fs
      where fs.folder.id = :folderId
        and fs.sharedWith.id = :userId
    """)
    boolean existsByFolderIdAndSharedWithId(@Param("folderId") UUID folderId, @Param("userId") UUID userId);
}