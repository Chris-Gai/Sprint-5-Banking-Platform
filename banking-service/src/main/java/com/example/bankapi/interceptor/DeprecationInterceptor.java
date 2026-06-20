package com.example.bankapi.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class DeprecationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getRequestURI().startsWith("/api/v1/")) {
            response.setHeader("Deprecation", "true");
            response.setHeader("Sunset", "Wed, 31 Dec 2026 23:59:59 GMT");
            response.setHeader("Link", "</api/v2" + request.getRequestURI().substring("/api/v1".length()) + ">; rel=\"successor-version\"");
        }
        return true;
    }
}
