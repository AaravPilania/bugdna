package com.bugdna.auth;

import com.bugdna.auth.dto.LoginRequest;
import com.bugdna.auth.dto.LoginResponse;
import com.bugdna.config.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final DevTokenStore tokenStore;

    @Value("${bugdna.auth.dev-user:dev}")
    private String devUser;

    @Value("${bugdna.auth.dev-password:password123}")
    private String devPassword;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthController(DevTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public AuthController(DevTokenStore tokenStore, String devUser, String devPassword) {
        this.tokenStore = tokenStore;
        this.devUser = devUser;
        this.devPassword = devPassword;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        if (devUser.equals(request.username()) && devPassword.equals(request.password())) {
            String displayName = request.username().equalsIgnoreCase("dev") ? "Lead Developer" : request.username();
            DevTokenStore.TokenInfo tokenInfo = tokenStore.createToken(request.username(), displayName);
            return ResponseEntity.ok(new LoginResponse(tokenInfo.token(), tokenInfo.displayName()));
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        problem.setTitle("Authentication Failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            tokenStore.removeToken(token);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        DevTokenStore.TokenInfo tokenInfo = (DevTokenStore.TokenInfo) request.getAttribute(AuthInterceptor.USER_ATTR);
        if (tokenInfo == null) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
        }
        return ResponseEntity.ok(Map.of(
                "username", tokenInfo.username(),
                "displayName", tokenInfo.displayName(),
                "expiresAt", tokenInfo.expiresAt().toString()
        ));
    }
}

