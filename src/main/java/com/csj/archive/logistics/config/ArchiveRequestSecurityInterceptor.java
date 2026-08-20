package com.csj.archive.logistics.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@Component
public class ArchiveRequestSecurityInterceptor implements HandlerInterceptor {
    private static final String AUTHORIZATION = "Authorization";
    private static final String SOURCE = "X-Archive-Source-System";
    private static final String SCOPE = "X-Archive-Service-Scope";
    private final ArchiveSecurityProperties properties;

    public ArchiveRequestSecurityInterceptor(ArchiveSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.isEnabled() || HttpMethod.OPTIONS.matches(request.getMethod()) || isPublic(request)) {
            return true;
        }
        String requiredScope = requiredScope(request);
        String source = request.getHeader(SOURCE);
        String scope = request.getHeader(SCOPE);
        String token = bearerToken(request.getHeader(AUTHORIZATION));
        if (!StringUtils.hasText(token) || !StringUtils.hasText(source) || !StringUtils.hasText(scope)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Archive service credentials are required");
            return false;
        }
        source = source.trim().toLowerCase();
        scope = canonicalScope(scope);
        if (!properties.getAllowedSources().contains(source) || !requiredScope.equals(scope)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Archive service identity or scope is not allowed");
            return false;
        }
        String expectedToken = "admin:operate".equals(requiredScope) ? properties.getAdminServiceToken()
                : "authenticated:read".equals(requiredScope) ? properties.getReadToken() : properties.getInternalServiceToken();
        if (!StringUtils.hasText(expectedToken) || !constantTimeEquals(expectedToken, token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Archive service token is invalid");
            return false;
        }
        return true;
    }

    private boolean isPublic(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/actuator/health".equals(uri) || "/actuator/info".equals(uri)
                || (HttpMethod.GET.matches(request.getMethod()) && Set.of(
                "/api/operations/summary", "/api/routes/summary", "/api/runtime/status", "/api/runtime-events/recent"
        ).contains(uri));
    }

    private String requiredScope(HttpServletRequest request) {
        if (HttpMethod.POST.matches(request.getMethod()) && request.getRequestURI().startsWith("/api/events/nexus")) {
            return "logistics:ingest";
        }
        if (HttpMethod.GET.matches(request.getMethod())) {
            return "authenticated:read";
        }
        return "admin:operate";
    }

    private String bearerToken(String header) {
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
    }

    private String canonicalScope(String scope) {
        if ("runtime:read".equals(scope) || "ledger:read".equals(scope)) return "authenticated:read";
        return scope;
    }

    private boolean constantTimeEquals(String expected, String provided) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }
}
