package com.asusoftware.SafeDocs_api.web;

import com.asusoftware.SafeDocs_api.auth.SimplePrincipal;
import com.asusoftware.SafeDocs_api.domain.Document;
import com.asusoftware.SafeDocs_api.domain.User;
import com.asusoftware.SafeDocs_api.dto.CreateDocumentRequest;
import com.asusoftware.SafeDocs_api.dto.DocumentResponse;
import com.asusoftware.SafeDocs_api.repo.DocumentRepository;
import com.asusoftware.SafeDocs_api.service.DocumentService;
import com.asusoftware.SafeDocs_api.service.StorageService;
import com.asusoftware.SafeDocs_api.utils.FileNameSanitizer;
import com.asusoftware.SafeDocs_api.utils.MimeSniffer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService docService;
    private final DocumentRepository docs;
    private final StorageService storage;
    private final com.asusoftware.SafeDocs_api.auth.CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<DocumentResponse> create(@AuthenticationPrincipal SimplePrincipal me,
                                                   @RequestBody @Valid CreateDocumentRequest req) {
        User user = currentUser.require(me);
        Document d = docService.createMeta(user, req.title(), req.folderId(), req.expiresAt(), req.tags());
        return ResponseEntity.ok(new DocumentResponse(d.getId(), d.getTitle(),
                d.getFolder() != null ? d.getFolder().getId() : null,
                d.getFilePath(), d.getMimeType(), d.getFileSize(), d.getExpiresAt()));
    }

    @PostMapping("/{id}/file")
    public ResponseEntity<?> upload(@AuthenticationPrincipal SimplePrincipal me,
                                    @PathVariable UUID id,
                                    @RequestParam("file") MultipartFile file) throws Exception {
        var user = currentUser.require(me);
        var d = docs.findById(id).orElseThrow();
        if (!d.getUser().getId().equals(user.getId())) return ResponseEntity.status(403).build();

        // Validări
        if (file.isEmpty()) return ResponseEntity.badRequest().body(java.util.Map.of("error","empty_file"));
        if (file.getSize() > 50 * 1024 * 1024) return ResponseEntity.badRequest().body(java.util.Map.of("error","file_too_large"));

        String filename = FileNameSanitizer.sanitize(file.getOriginalFilename());
        String relPath = storage.save(file, user.getId(), d.getId(), 1, filename);

        // Checksum + MIME
        byte[] bytes = file.getBytes();
        String mime = MimeSniffer.detect(bytes, file.getContentType());
        String sha256 = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));

        d.setFilePath(relPath);
        d.setMimeType(mime);
        d.setFileSize(file.getSize());
        d.setChecksumSha256(sha256);
        docs.save(d);

        return ResponseEntity.ok(java.util.Map.of("path", relPath, "mime", mime, "size", file.getSize()));
    }


    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> get(@PathVariable UUID id) {
        var d = docs.findById(id).orElseThrow();
        return ResponseEntity.ok(new DocumentResponse(d.getId(), d.getTitle(),
                d.getFolder() != null ? d.getFolder().getId() : null,
                d.getFilePath(), d.getMimeType(), d.getFileSize(), d.getExpiresAt()));
    }
}