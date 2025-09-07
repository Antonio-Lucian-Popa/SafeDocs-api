package com.asusoftware.SafeDocs_api.service;


import com.asusoftware.SafeDocs_api.domain.Document;
import com.asusoftware.SafeDocs_api.domain.DocumentVersion;
import com.asusoftware.SafeDocs_api.domain.Folder;
import com.asusoftware.SafeDocs_api.domain.User;
import com.asusoftware.SafeDocs_api.repo.DocumentRepository;
import com.asusoftware.SafeDocs_api.repo.DocumentVersionRepository;
import com.asusoftware.SafeDocs_api.repo.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository docs;
    private final DocumentVersionRepository versions;
    private final FolderRepository folders;

    @Transactional
    public Document createMeta(User user, String title, UUID folderId, Instant expiresAt, java.util.Map<String,Object> tags) {
        Folder folder = null;
        if (folderId != null) folder = folders.findById(folderId).orElseThrow(() -> new IllegalArgumentException("Folder not found"));


        var d = Document.builder()
                .user(user) // setezi user-ul în controller din principal, sau încarci entitatea User
                .folder(folder)
                .title(title)
                .filePath("") // setăm după upload
                .mimeType(null)
                .fileSize(null)
                .checksumSha256(null)
                .tags(tags)
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        d = docs.save(d);

        var v1 = DocumentVersion.builder()
                .document(d)
                .versionNo(1)
                .filePath("") // după upload
                .createdAt(Instant.now())
                .build();
        versions.save(v1);

        return d;
    }
}
