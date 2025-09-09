package com.asusoftware.SafeDocs_api.web;

import com.asusoftware.SafeDocs_api.auth.CurrentUser;
import com.asusoftware.SafeDocs_api.auth.SimplePrincipal;
import com.asusoftware.SafeDocs_api.domain.Document;
import com.asusoftware.SafeDocs_api.domain.DocumentVersion;
import com.asusoftware.SafeDocs_api.dto.DocumentVersionItem;
import com.asusoftware.SafeDocs_api.repo.DocumentRepository;
import com.asusoftware.SafeDocs_api.repo.DocumentVersionRepository;
import com.asusoftware.SafeDocs_api.service.DocumentVersioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documents/{id}/versions")
@RequiredArgsConstructor
public class DocumentVersionController {

    private final DocumentRepository docs;
    private final DocumentVersionRepository versions;
    private final DocumentVersioningService versioning;
    private final CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<?> uploadNew(@AuthenticationPrincipal SimplePrincipal me,
                                       @PathVariable UUID id,
                                       @RequestParam("file") MultipartFile file) throws Exception {
        var user = currentUser.require(me);
        Document d = docs.findById(id).orElseThrow();
        if (!d.getUser().getId().equals(user.getId())) return ResponseEntity.status(403).build();
        if (file.isEmpty()) return ResponseEntity.badRequest().body(java.util.Map.of("error","empty_file"));

        DocumentVersion v = versioning.addVersion(user.getId(), id, file);
        return ResponseEntity.ok(java.util.Map.of(
                "versionNo", v.getVersionNo(),
                "filePath", v.getFilePath(),
                "mime", v.getMimeType(),
                "size", v.getFileSize()
        ));
    }

    @GetMapping
    public ResponseEntity<List<DocumentVersionItem>> list(
            @org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "id") UUID userId,
            @PathVariable UUID id
    ) {
        if (userId == null) return ResponseEntity.status(401).build();
        var items = versions.findItemsForUserAndDocument(userId, id);
        if (items.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(items);
    }


    @PostMapping("/{versionNo}/revert")
    public ResponseEntity<?> revert(@AuthenticationPrincipal SimplePrincipal me,
                                    @PathVariable UUID id,
                                    @PathVariable int versionNo) {
        var user = currentUser.require(me);
        Document d = docs.findById(id).orElseThrow();
        if (!d.getUser().getId().equals(user.getId())) return ResponseEntity.status(403).build();
        var updated = versioning.revertToVersion(id, versionNo);
        return ResponseEntity.ok(java.util.Map.of(
                "currentPath", updated.getFilePath(),
                "versionSetTo", versionNo
        ));
    }
}
