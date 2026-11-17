package com.bugdna.config;

import com.bugdna.auth.DevTokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ATTR = "bugdna_user";
    private final DevTokenStore tokenStore;

    public AuthInterceptor(DevTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Skip preflight CORS requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(7).trim();
        var tokenInfoOpt = tokenStore.validateToken(token);
        if (tokenInfoOpt.isEmpty()) {
            sendUnauthorized(response, "Token is expired or invalid");
            return false;
        }

        request.setAttribute(USER_ATTR, tokenInfoOpt.get());
        return true;
    }

    private void sendUnauthorized(HttpServletResponse response, String detail) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String json = """
            {
              "type": "about:blank",
              "title": "Unauthorized",
              "status": 401,
              "detail": "%s"
            }
            """.formatted(detail);
        response.getWriter().write(json);
    }
}

