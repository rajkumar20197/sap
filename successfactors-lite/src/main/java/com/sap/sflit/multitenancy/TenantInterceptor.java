package com.sap.sflit.multitenancy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * TenantInterceptor — Intercepts every HTTP request to extract and store the tenant ID.
 *
 * INTERVIEW TALKING POINT:
 * HandlerInterceptor is a Spring MVC hook with 3 lifecycle methods:
 *   - preHandle:   runs BEFORE the controller. Use to: auth, rate limiting, tenant extraction.
 *   - postHandle:  runs AFTER controller but BEFORE view rendering. Rarely used in REST APIs.
 *   - afterCompletion: runs AFTER response sent, even on exceptions. Use for cleanup.
 *
 * SAP PATTERN:
 * SAP SuccessFactors routes all traffic through middleware that validates tenant licenses
 * before any business logic runs. This interceptor mimics that pattern.
 *
 * SECURITY NOTE:
 * In production, the tenant ID would come from a JWT claim (not a plain header)
 * to prevent spoofing. The X-Tenant-ID header approach is for demo purposes.
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String DEFAULT_TENANT = "default";

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        String tenantId = request.getHeader(TENANT_HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            // Fallback to default tenant for demo purposes
            // In production: return 401 or 400 with error message
            tenantId = DEFAULT_TENANT;
        }

        TenantContext.setCurrentTenant(tenantId);
        return true; // true = continue processing; false = abort request
    }

    @Override
    public void postHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            ModelAndView modelAndView
    ) {
        // No-op for REST APIs
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex
    ) {
        // CRITICAL: Always clear ThreadLocal after request to prevent tenant leakage
        // between requests on the same thread in the servlet thread pool
        TenantContext.clear();
    }
}
