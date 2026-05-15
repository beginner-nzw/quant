package com.quant.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(SecurityConstants.HEADER_USER_ID);
        String userRole = request.getHeader(SecurityConstants.HEADER_USER_ROLE);

        if (userId == null || userId.isBlank()) {
            userId = SecurityConstants.DEFAULT_USER_ID;
        }
        if (userRole == null || userRole.isBlank()) {
            userRole = SecurityConstants.DEFAULT_USER_ROLE;
        }

        UserContext.set(userId, userRole);
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}