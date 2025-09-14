package com.asusoftware.SafeDocs_api.web;

import com.asusoftware.SafeDocs_api.auth.CurrentUser;
import com.asusoftware.SafeDocs_api.auth.SimplePrincipal;
import com.asusoftware.SafeDocs_api.domain.Folder;
import com.asusoftware.SafeDocs_api.domain.FolderShare;
import com.asusoftware.SafeDocs_api.domain.User;
import com.asusoftware.SafeDocs_api.dto.*;
import com.asusoftware.SafeDocs_api.repo.FolderRepository;
import com.asusoftware.SafeDocs_api.repo.FolderShareRepository;
import com.asusoftware.SafeDocs_api.repo.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
public class FolderController {
    private final FolderRepository folders;
    private final FolderShareRepository folderShares;
    private final UserRepository users;
    private final CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<FolderResponse> create(@AuthenticationPrincipal SimplePrincipal me,
                                                 @RequestBody @Valid CreateFolderRequest req) {
        User user = currentUser.require(me);
        Folder parent = null;

        if (req.parentId() != null) {
            parent = folders.findById(req.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent not found"));
            // ✅ NU lăsa pe altcineva să creeze în folderul altui user
            if (!parent.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }

        Folder f = Folder.builder()
                .user(user)
                .parent(parent)
                .name(req.name())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        f = folders.save(f);
        return ResponseEntity.ok(new FolderResponse(
                f.getId(), f.getName(), f.getParent() != null ? f.getParent().getId() : null));
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> list(@AuthenticationPrincipal SimplePrincipal me,
                                                     @RequestParam(required = false) UUID parentId) {
        User user = currentUser.require(me);
        var list = folders.findByUserIdAndParentIdOrderByNameAsc(user.getId(), parentId).stream()
                .map(f -> new FolderResponse(f.getId(), f.getName(),
                        f.getParent() != null ? f.getParent().getId() : null))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{folderId}/access")
    public ResponseEntity<FolderAccessResponse> access(@AuthenticationPrincipal SimplePrincipal me,
                                                       @PathVariable UUID folderId) {
        var user = currentUser.require(me);
        var folder = folders.findById(folderId).orElse(null);
        if (folder == null) return ResponseEntity.notFound().build();

        boolean isOwner = folder.getUser().getId().equals(user.getId());
        boolean shared  = folderShares.existsByFolder_IdAndSharedWith_Id(folderId, user.getId());

        boolean canWrite = isOwner;
        if (!isOwner && shared) {
            var share = folderShares.findByFolder_IdAndSharedWith_Id(folderId, user.getId()).orElse(null);
            canWrite = share != null && "WRITE".equalsIgnoreCase(share.getPermission());
        }
        return ResponseEntity.ok(new FolderAccessResponse(isOwner, canWrite));
    }

    @GetMapping("/{folderId}/shares")
    public ResponseEntity<List<ShareItemResponse>> listShares(@AuthenticationPrincipal SimplePrincipal me,
                                                              @PathVariable UUID folderId) {
        var user = currentUser.require(me);
        var folder = folders.findById(folderId).orElse(null);
        if (folder == null) return ResponseEntity.notFound().build();
        if (!folder.getUser().getId().equals(user.getId())) return ResponseEntity.status(403).build();

        var items = folderShares.listSharesForFolder(folderId).stream()
                .map(s -> new ShareItemResponse(
                        s.getId(),
                        s.getSharedWith().getId(),
                        s.getSharedWith().getEmail(),
                        s.getPermission(),
                        s.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{folderId}/shares")
    public ResponseEntity<ShareItemResponse> createShare(@AuthenticationPrincipal SimplePrincipal me,
                                                         @PathVariable UUID folderId,
                                                         @RequestBody @Valid CreateShareRequest req) {
        var user = currentUser.require(me);
        var folder = folders.findById(folderId).orElse(null);
        if (folder == null) return ResponseEntity.notFound().build();
        if (!folder.getUser().getId().equals(user.getId())) return ResponseEntity.status(403).build();

        var target = users.findByEmailIgnoreCase(req.targetEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found by email"));
        if (target.getId().equals(user.getId())) return ResponseEntity.badRequest().build();

        // upsert cu by-email (dacă există deja)
        var existing = folderShares.findByFolderIdAndSharedWithEmail(folderId, req.targetEmail());
        var fs = existing.isEmpty() ? null : existing.get(0);

        if (fs == null) {
            fs = FolderShare.builder()
                    .folder(folder)
                    .sharedWith(target)
                    .permission(req.permission().toUpperCase())
                    .createdBy(user)
                    .createdAt(Instant.now())
                    .build();
        } else {
            fs.setPermission(req.permission().toUpperCase());
        }

        fs = folderShares.save(fs);
        return ResponseEntity.ok(new ShareItemResponse(
                fs.getId(), target.getId(), target.getEmail(), fs.getPermission(), fs.getCreatedAt()));
    }

    @DeleteMapping("/{folderId}/shares/{targetUserId}")
    public ResponseEntity<Void> revokeShare(@AuthenticationPrincipal SimplePrincipal me,
                                            @PathVariable UUID folderId,
                                            @PathVariable UUID targetUserId) {
        var user = currentUser.require(me);
        var folder = folders.findById(folderId).orElse(null);
        if (folder == null) return ResponseEntity.notFound().build();
        if (!folder.getUser().getId().equals(user.getId())) return ResponseEntity.status(403).build();

        int deleted = folderShares.deleteShare(folderId, targetUserId);
        return deleted > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }


    @DeleteMapping("/{folderId}/shares/by-email")
    public ResponseEntity<Void> revokeShareByEmail(@AuthenticationPrincipal SimplePrincipal me,
                                                   @PathVariable UUID folderId,
                                                   @RequestParam String email) {
        var user = currentUser.require(me);
        var folder = folders.findById(folderId).orElse(null);
        if (folder == null) return ResponseEntity.notFound().build();
        if (!folder.getUser().getId().equals(user.getId())) return ResponseEntity.status(403).build();

        folderShares.findByFolderIdAndSharedWithEmail(folderId, email)
                .forEach(folderShares::delete);
        return ResponseEntity.noContent().build();
    }
}
