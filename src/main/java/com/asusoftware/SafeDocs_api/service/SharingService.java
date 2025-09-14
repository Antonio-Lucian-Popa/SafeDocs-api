package com.asusoftware.SafeDocs_api.service;

import com.asusoftware.SafeDocs_api.domain.FolderShare;
import com.asusoftware.SafeDocs_api.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service @RequiredArgsConstructor
public class SharingService {

    private final FolderShareRepository folderShareRepo;
    private final FolderRepository folderRepo; // presupun că-l ai
    private final UserRepository userRepo;     // presupun că-l ai

    public boolean canReadFolder(UUID currentUserId, UUID folderId) {
        var folder = folderRepo.findById(folderId).orElse(null);
        if (folder == null) return false;
        if (folder.getUser().getId().equals(currentUserId)) return true;
        return folderShareRepo.hasAnyAccess(currentUserId, folderId);
    }

    public boolean canWriteFolder(UUID currentUserId, UUID folderId) {
        var folder = folderRepo.findById(folderId).orElse(null);
        if (folder == null) return false;
        if (folder.getUser().getId().equals(currentUserId)) return true;
        return folderShareRepo.hasWriteAccess(currentUserId, folderId);
    }

    @Transactional
    public FolderShare shareFolder(UUID ownerId, UUID folderId, String targetEmail, String permission) {
        var folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("folder_not_found"));
        if (!folder.getUser().getId().equals(ownerId)) {
            throw new IllegalArgumentException("not_owner");
        }
        var target = userRepo.findByEmail(targetEmail)
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        var existing = folderShareRepo.findByFolder_IdAndSharedWith_Id(folderId, target.getId());
        if (existing.isPresent()) {
            var fs = existing.get();
            fs.setPermission(permission);
            return folderShareRepo.save(fs);
        }

        var fs = FolderShare.builder()
                .folder(folder)
                .sharedWith(target)
                .permission(permission)
                .createdBy(folder.getUser())
                .build();
        return folderShareRepo.save(fs);
    }


    public List<FolderShare> listShares(UUID ownerId, UUID folderId) {
        var folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("folder_not_found"));
        if (!folder.getUser().getId().equals(ownerId)) {
            throw new IllegalArgumentException("not_owner");
        }
        return folderShareRepo.listSharesForFolder(folderId);
    }
}
