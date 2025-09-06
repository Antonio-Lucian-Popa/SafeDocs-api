package com.asusoftware.SafeDocs_api.web;

import com.asusoftware.SafeDocs_api.auth.CurrentUser;
import com.asusoftware.SafeDocs_api.auth.SimplePrincipal;
import com.asusoftware.SafeDocs_api.repo.DocumentQueryRepository;
import com.asusoftware.SafeDocs_api.repo.ExpiringSoonProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentsQueryController {

    private final DocumentQueryRepository queryRepo;
    private final CurrentUser currentUser;

    @GetMapping("/expiring-soon")
    public ResponseEntity<List<ExpiringSoonProjection>> expiringSoon(@AuthenticationPrincipal SimplePrincipal me) {
        var user = currentUser.require(me);
        return ResponseEntity.ok(queryRepo.findExpiringSoon(user.getId()));
    }
}
