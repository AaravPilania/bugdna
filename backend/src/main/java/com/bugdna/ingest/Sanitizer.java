package com.bugdna.ingest;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class Sanitizer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b");
    private static final Pattern BEARER_PATTERN = Pattern.compile("Bearer\\s+[A-Za-z0-9._\\-~+/]+=*", Pattern.CASE_INSENSITIVE);
    private static final Pattern LONG_TOKEN_PATTERN = Pattern.compile("\\b[0-9a-fA-F]{32,64}\\b");
    private static final Pattern DIGIT_RUN_PATTERN = Pattern.compile("\\b\\d{8,}\\b");
    private static final Pattern NUMERIC_ID_PATH = Pattern.compile("/\\d+(?=/|$)");
    private static final Pattern UUID_PATH = Pattern.compile("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)");

    public String sanitizeText(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String s = text.trim();
        s = EMAIL_PATTERN.matcher(s).replaceAll("[email]");
        s = BEARER_PATTERN.matcher(s).replaceAll("Bearer [token]");
        s = LONG_TOKEN_PATTERN.matcher(s).replaceAll("[token]");
        s = DIGIT_RUN_PATTERN.matcher(s).replaceAll("[number]");

        if (s.length() > maxLength) {
            s = s.substring(0, maxLength);
        }
        return s;
    }

    public String sanitizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String s = path.trim();
        int q = s.indexOf('?');
        if (q >= 0) {
            s = s.substring(0, q);
        }
        int h = s.indexOf('#');
        if (h >= 0) {
            s = s.substring(0, h);
        }
        s = UUID_PATH.matcher(s).replaceAll("/:id");
        s = NUMERIC_ID_PATH.matcher(s).replaceAll("/:id");
        s = s.replaceAll("/+", "/");
        if (s.length() > 1 && s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}

