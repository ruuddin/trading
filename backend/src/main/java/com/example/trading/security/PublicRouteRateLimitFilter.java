package com.example.trading.security;

import com.example.trading.service.PublicRateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PublicRouteRateLimitFilter extends OncePerRequestFilter {

    private final PublicRateLimiterService limiter;

    @Value("${app.rate-limit.public.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.public.max-requests:120}")
    private int maxRequests;

    @Value("${app.rate-limit.public.window-seconds:60}")
    private int windowSeconds;

    public PublicRouteRateLimitFilter(PublicRateLimiterService limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) return true;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = request.getRequestURI();
        return !(path.startsWith("/api/stocks")
            || path.startsWith("/api/metrics")
            || path.equals("/api/auth/login")
            || path.equals("/api/auth/register")
            || path.equals("/api/billing/webhook"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String clientIp = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        String key = clientIp + "|" + request.getMethod() + "|" + request.getRequestURI();

        boolean allowed = limiter.allow(key, maxRequests, windowSeconds * 1000L);
        if (!allowed) {
            response.setStatus(429);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("rate limit exceeded");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
