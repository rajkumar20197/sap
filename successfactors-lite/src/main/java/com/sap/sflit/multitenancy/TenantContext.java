package com.sap.sflit.multitenancy;

/**
 * TenantContext — Thread-safe storage for the current request's tenant ID.
 *
 * INTERVIEW TALKING POINT — Multi-Tenancy Pattern:
 *
 * ThreadLocal stores values per-thread, making it perfect for web applications
 * where each request runs on its own thread (Servlet thread pool model).
 *
 * FLOW:
 *   1. HTTP Request arrives → TenantInterceptor reads X-Tenant-ID header
 *   2. TenantInterceptor calls TenantContext.setCurrentTenant("company-abc")
 *   3. Repository queries automatically add WHERE tenant_id = 'company-abc'
 *   4. After response: TenantInterceptor calls TenantContext.clear() to prevent leaks
 *
 * WHY CLEAR IS CRITICAL:
 *   Thread pools reuse threads. If you don't clear ThreadLocal after a request,
 *   the NEXT request on that thread will inherit the PREVIOUS request's tenant.
 *   This would be a serious data leak in a multi-tenant system!
 *
 * ALTERNATIVE: Spring Security's SecurityContextHolder uses the same pattern.
 */
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new InheritableThreadLocal<>();

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
