package com.asusoftware.SafeDocs_api.auth;

import com.asusoftware.SafeDocs_api.domain.AuthProvider;
import com.asusoftware.SafeDocs_api.domain.User;
import com.asusoftware.SafeDocs_api.repo.UserRepository;
import com.asusoftware.SafeDocs_api.service.RefreshTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleTokenVerifier googleVerifier;
    private final UserRepository users;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;

    public record GoogleLoginRequest(@NotBlank String idToken) {}
    public record AuthResponse(String accessToken) {}

    public record TokenPair(String accessToken, String refreshToken) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}

    @PostMapping("/google")
    public ResponseEntity<TokenPair> google(@RequestBody GoogleLoginRequest body) throws Exception {
        var payload = googleVerifier.verify(body.idToken());
        if (payload == null) return ResponseEntity.status(401).build();

        String email = payload.getEmail();
        String sub   = payload.getSubject();
        String name  = (String) payload.get("name");

        var user = users.findByGoogleSub(sub).orElseGet(() ->
                users.findByEmail(email).orElseGet(() -> users.save(User.builder()
                        .email(email).displayName(name).provider(AuthProvider.GOOGLE)
                        .googleSub(sub).active(true).createdAt(java.time.Instant.now()).updatedAt(java.time.Instant.now()).build())));

        if (user.getGoogleSub() == null) { user.setGoogleSub(sub); user.setProvider(AuthProvider.GOOGLE); users.save(user); }

        var access = jwt.issueAccess(user.getId(), user.getEmail());
        var rtoken = refreshTokens.mint(user).getToken();
        return ResponseEntity.ok(new TokenPair(access, rtoken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestBody @Valid RefreshRequest req) {
        var rt = refreshTokens.requireValid(req.refreshToken());
        var user = rt.getUser();
        // rotate: revoke old, mint new
        refreshTokens.revoke(rt);
        var newRt = refreshTokens.mint(user);
        var access = jwt.issueAccess(user.getId(), user.getEmail());
        return ResponseEntity.ok(new TokenPair(access, newRt.getToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshRequest req) {
        try {
            var rt = refreshTokens.requireValid(req.refreshToken());
            refreshTokens.revoke(rt);
        } catch (Exception ignored) { /* idempotent */ }
        return ResponseEntity.noContent().build();
    }

}
