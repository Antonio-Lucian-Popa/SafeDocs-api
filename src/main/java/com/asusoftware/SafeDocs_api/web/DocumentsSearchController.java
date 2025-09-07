package com.asusoftware.SafeDocs_api.web;

import com.asusoftware.SafeDocs_api.auth.CurrentUser;
import com.asusoftware.SafeDocs_api.auth.SimplePrincipal;
import com.asusoftware.SafeDocs_api.domain.Document;
import com.asusoftware.SafeDocs_api.repo.DocumentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentsSearchController {

    private final CurrentUser currentUser;
    private final DocumentRepository docs;

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/search")
    public ResponseEntity<List<Document>> search(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tagKey,
            @RequestParam(required = false) String tagValue) {

        List<Document> list;

        if (tagKey != null && tagValue != null) {
            list = docs.findByUserIdAndTag(userId, tagKey, tagValue);
        } else if (q != null && !q.isBlank()) {
            list = docs.findByUserIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(userId, q);
        } else {
            list = docs.findTop50ByUserIdOrderByCreatedAtDesc(userId);
        }

        return ResponseEntity.ok(list);
    }

}
