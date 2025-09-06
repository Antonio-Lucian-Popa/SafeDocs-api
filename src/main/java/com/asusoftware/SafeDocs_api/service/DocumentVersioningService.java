package com.asusoftware.SafeDocs_api.service;

import com.asusoftware.SafeDocs_api.domain.Document;
import com.asusoftware.SafeDocs_api.domain.DocumentVersion;
import com.asusoftware.SafeDocs_api.repo.DocumentRepository;
import com.asusoftware.SafeDocs_api.repo.DocumentVersionRepository;
import com.asusoftware.SafeDocs_api.utils.FileNameSanitizer;
import com.asusoftware.SafeDocs_api.utils.MimeSniffer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentVersioningService {
    private final DocumentRepository docs;
    private final DocumentVersionRepository versions;
    private final StorageService storage;

    @Transactional
    public DocumentVersion addVersion(UUID userId, UUID docId, MultipartFile file) throws Exception {
        Document d = docs.findById(docId).orElseThrow();

        // (validările de proprietar fă-le în controller)
        int next = versions.findMaxVersionNoByDocumentId(docId) + 1;

        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String safe = FileNameSanitizer.sanitize(original);

        String relPath = storage.save(file, userId, docId, next, safe);

        byte[] bytes = file.getBytes();
        String mime = MimeSniffer.detect(bytes, file.getContentType());
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));

        var v = DocumentVersion.builder()
                .document(d)
                .versionNo(next)
                .filePath(relPath)
                .mimeType(mime)
                .fileSize(file.getSize())
                .checksumSha256(sha256)
                .createdAt(java.time.Instant.now())
                .build();
        versions.save(v);

        // setează ca “curentă”
        d.setFilePath(relPath);
        d.setMimeType(mime);
        d.setFileSize(file.getSize());
        d.setChecksumSha256(sha256);
        docs.save(d);

        return v;
    }

    @Transactional
    public Document revertToVersion(UUID docId, int versionNo) {
        var d = docs.findById(docId).orElseThrow();
        var v = versions.findByDocumentIdOrderByVersionNoDesc(docId).stream()
                .filter(x -> x.getVersionNo() == versionNo).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("version_not_found"));

        d.setFilePath(v.getFilePath());
        d.setMimeType(v.getMimeType());
        d.setFileSize(v.getFileSize());
        d.setChecksumSha256(v.getChecksumSha256());
        return docs.save(d);
    }
}