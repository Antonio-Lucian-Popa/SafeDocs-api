package com.asusoftware.SafeDocs_api.auth;

import com.asusoftware.SafeDocs_api.domain.AuthProvider;
import com.asusoftware.SafeDocs_api.domain.User;
import com.asusoftware.SafeDocs_api.repo.UserRepository;
import com.asusoftware.SafeDocs_api.service.RefreshTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder; // <— injectat

    public record GoogleLoginRequest(@NotBlank String idToken) {}
    public record TokenPair(String accessToken, String refreshToken) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}

    // ====== MANUAL REGISTER/LOGIN ======

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            String displayName
    ) {}

    @PostMapping("/register")
    public ResponseEntity<TokenPair> register(@RequestBody @Valid RegisterRequest req) {
        var existing = users.findByEmail(req.email()).orElse(null);
        if (existing != null) {
            return ResponseEntity.status(409).build(); // email already used
        }

        var now = Instant.now();
        var user = User.builder()
                .email(req.email())
                .displayName(req.displayName() != null ? req.displayName() : req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .provider(AuthProvider.LOCAL)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        user = users.save(user);

        var access = jwt.issueAccess(user.getId(), user.getEmail());
        var rtoken = refreshTokens.mint(user).getToken();
        return ResponseEntity.ok(new TokenPair(access, rtoken));
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@RequestBody @Valid LoginRequest req) {
        var user = users.findByEmail(req.email()).orElse(null);
        if (user == null || user.getPasswordHash() == null) {
            return ResponseEntity.status(401).build();
        }
        if (!user.isActive()) {
            return ResponseEntity.status(403).build();
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401).build();
        }

        var access = jwt.issueAccess(user.getId(), user.getEmail());
        var rtoken = refreshTokens.mint(user).getToken();
        return ResponseEntity.ok(new TokenPair(access, rtoken));
    }

    // ====== GOOGLE (existente) ======

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
        } catch (Exception ignored) { }
        return ResponseEntity.noContent().build();
    }
}
