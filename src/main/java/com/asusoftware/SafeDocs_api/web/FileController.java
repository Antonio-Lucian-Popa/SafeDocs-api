package com.asusoftware.SafeDocs_api.web;

import com.asusoftware.SafeDocs_api.auth.CurrentUser;
import com.asusoftware.SafeDocs_api.auth.SimplePrincipal;
import com.asusoftware.SafeDocs_api.config.AppProperties;
import com.asusoftware.SafeDocs_api.domain.Document;
import com.asusoftware.SafeDocs_api.repo.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final DocumentRepository docs;
    private final CurrentUser currentUser;
    private final AppProperties appProps;

    @GetMapping("/{documentId}/download")
    public ResponseEntity<FileSystemResource> download(@AuthenticationPrincipal SimplePrincipal me,
                                                       @PathVariable UUID documentId) throws IOException {
        var user = currentUser.require(me);
        Document d = docs.findById(documentId).orElseThrow();
        if (!d.getUser().getId().equals(user.getId())) return ResponseEntity.status(403).build();

        Path abs = Paths.get(appProps.getStorage().getLocation()).resolve(d.getFilePath()).normalize();
        var resource = new FileSystemResource(abs);
        if (!resource.exists()) return ResponseEntity.notFound().build();

        String filename = abs.getFileName().toString();
        var cd = ContentDisposition.attachment().filename(filename).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(cd);
        headers.setContentType(MediaType.parseMediaType(d.getMimeType() != null ? d.getMimeType() : MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentLength(resource.contentLength());

        return ResponseEntity.ok().headers(headers).body(resource);
    }
}