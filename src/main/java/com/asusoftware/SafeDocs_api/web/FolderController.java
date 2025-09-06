package com.asusoftware.SafeDocs_api.web;

import com.asusoftware.SafeDocs_api.auth.CurrentUser;
import com.asusoftware.SafeDocs_api.auth.SimplePrincipal;
import com.asusoftware.SafeDocs_api.domain.Folder;
import com.asusoftware.SafeDocs_api.domain.User;
import com.asusoftware.SafeDocs_api.dto.CreateFolderRequest;
import com.asusoftware.SafeDocs_api.dto.FolderResponse;
import com.asusoftware.SafeDocs_api.repo.FolderRepository;
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
    private final CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<FolderResponse> create(@AuthenticationPrincipal SimplePrincipal me,
                                                 @RequestBody @Valid CreateFolderRequest req) {
        User user = currentUser.require(me);
        Folder parent = null;
        if (req.parentId() != null) {
            parent = folders.findById(req.parentId()).orElseThrow(() -> new IllegalArgumentException("Parent not found"));
        }
        Folder f = Folder.builder()
                .user(user)
                .parent(parent)
                .name(req.name())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        f = folders.save(f);
        return ResponseEntity.ok(new FolderResponse(f.getId(), f.getName(), f.getParent() != null ? f.getParent().getId() : null));
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> list(@AuthenticationPrincipal SimplePrincipal me,
                                                     @RequestParam(required = false) UUID parentId) {
        User user = currentUser.require(me);
        var list = folders.findByUserIdAndParentIdOrderByNameAsc(user.getId(), parentId).stream()
                .map(f -> new FolderResponse(f.getId(), f.getName(), f.getParent() != null ? f.getParent().getId() : null))
                .toList();
        return ResponseEntity.ok(list);
    }
}
