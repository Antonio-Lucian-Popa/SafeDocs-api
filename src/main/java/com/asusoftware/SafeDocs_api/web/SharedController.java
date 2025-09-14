package com.asusoftware.SafeDocs_api.web;

import com.asusoftware.SafeDocs_api.auth.CurrentUser;
import com.asusoftware.SafeDocs_api.auth.SimplePrincipal;
import com.asusoftware.SafeDocs_api.domain.Folder;
import com.asusoftware.SafeDocs_api.dto.DocumentListItem;
import com.asusoftware.SafeDocs_api.dto.FolderResponse;
import com.asusoftware.SafeDocs_api.repo.DocumentRepository;
import com.asusoftware.SafeDocs_api.repo.FolderShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/shared")
@RequiredArgsConstructor
public class SharedController {

    private final CurrentUser currentUser;
    private final FolderShareRepository folderShares;
    private final DocumentRepository docs;

    @GetMapping("/folders")
    public ResponseEntity<List<FolderResponse>> folders(@AuthenticationPrincipal SimplePrincipal me) {
        var user = currentUser.require(me);
        List<Folder> shared = folderShares.findSharedFoldersForUser(user.getId());
        var payload = shared.stream()
                .map(f -> new FolderResponse(
                        f.getId(),
                        f.getName(),
                        f.getParent() != null ? f.getParent().getId() : null
                ))
                .toList();
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentListItem>> documents(@AuthenticationPrincipal SimplePrincipal me) {
        var user = currentUser.require(me);
        return ResponseEntity.ok(docs.findSharedWithMe(user.getId()));
    }
}
