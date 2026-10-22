package com.example.maintenance.security;

import com.example.maintenance.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Without this, Spring Security's default behavior returns 403 Forbidden for BOTH
 * "no credentials at all" and "authenticated but wrong role" -- collapsing a real
 * semantic distinction a REST API client relies on. This entry point specifically
 * handles the "not authenticated at all" case and returns 401, matching what
 * AuthController's own tests expect. AccessDeniedException (wrong role, but
 * authenticated) still correctly returns 403 via GlobalExceptionHandler, unaffected
 * by this change.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Authentication is required to access this resource")
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}