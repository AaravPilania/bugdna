package com.bugdna.auth.dto;

public record LoginResponse(
    String token,
    String displayName
) {}

