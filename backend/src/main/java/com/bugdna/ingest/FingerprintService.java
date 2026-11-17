package com.bugdna.ingest;

import com.bugdna.bug.ErrorType;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

@Service
public class FingerprintService {

    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern HEX_ID_PATTERN = Pattern.compile("(?<=/)[0-9a-fA-F]{16,64}(?=(/|$))");
    private static final Pattern NUMERIC_ID_PATTERN = Pattern.compile("(?<=/)\\d+(?=(/|$))");
    private static final Pattern QUOTED_LITERAL_PATTERN = Pattern.compile("['\"`][^'\"`]*['\"`]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\b");
    private static final Pattern LINE_COL_PATTERN = Pattern.compile(":\\d+(:\\d+)?$");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    public String computeCanonical(
            ErrorType type,
            String method,
            String endpoint,
            Integer status,
            String errorCode,
            String errorName,
            String message,
            String topFrame
    ) {
        if (type == null) {
            type = ErrorType.JS_ERROR;
        }

        if (type == ErrorType.API_ERROR) {
            String normMethod = (method != null && !method.isBlank()) ? method.trim().toUpperCase() : "GET";
            String normEndpoint = normalizeEndpoint(endpoint);
            String normStatus = (status != null) ? String.valueOf(status) : "0";
            String normCode = (errorCode != null && !errorCode.isBlank()) ? errorCode.trim() : "-";
            return "v1|API_ERROR|" + normMethod + "|" + normEndpoint + "|" + normStatus + "|" + normCode;
        } else if (type == ErrorType.NETWORK_ERROR) {
            String normMethod = (method != null && !method.isBlank()) ? method.trim().toUpperCase() : "GET";
            String normEndpoint = normalizeEndpoint(endpoint);
            return "v1|NETWORK_ERROR|" + normMethod + "|" + normEndpoint;
        } else if (type == ErrorType.JS_ERROR || type == ErrorType.PROMISE_REJECTION) {
            String normName = (errorName != null && !errorName.isBlank()) ? errorName.trim() : "Error";
            String normMessage = normalizeMessage(message);
            String normFrame = normalizeTopFrame(topFrame);
            return "v1|" + type.name() + "|" + normName + "|" + normMessage + "|" + normFrame;
        } else {
            throw new IllegalArgumentException("Unsupported error type: " + type);
        }
    }

    public String computeFingerprint(String canonical) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "/";
        }
        String s = endpoint.trim();
        // Remove query params and fragment
        int qIdx = s.indexOf('?');
        if (qIdx >= 0) {
            s = s.substring(0, qIdx);
        }
        int hashIdx = s.indexOf('#');
        if (hashIdx >= 0) {
            s = s.substring(0, hashIdx);
        }
        s = s.toLowerCase();
        s = UUID_PATTERN.matcher(s).replaceAll("<id>");
        s = HEX_ID_PATTERN.matcher(s).replaceAll("<id>");
        s = NUMERIC_ID_PATTERN.matcher(s).replaceAll("<id>");
        s = s.replaceAll("/+", "/");
        if (s.length() > 1 && s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public String normalizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String s = message.trim().toLowerCase();
        s = QUOTED_LITERAL_PATTERN.matcher(s).replaceAll("<s>");
        s = NUMBER_PATTERN.matcher(s).replaceAll("<n>");
        s = WHITESPACE_PATTERN.matcher(s).replaceAll(" ").trim();
        return s;
    }

    public String normalizeTopFrame(String topFrame) {
        if (topFrame == null || topFrame.isBlank()) {
            return "";
        }
        String s = topFrame.trim().toLowerCase();
        // Strip line:col at end of frame
        s = LINE_COL_PATTERN.matcher(s).replaceAll("");
        s = WHITESPACE_PATTERN.matcher(s).replaceAll(" ").trim();
        return s;
    }
}

