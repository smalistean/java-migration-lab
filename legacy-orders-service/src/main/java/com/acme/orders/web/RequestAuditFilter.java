package com.acme.orders.web;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RequestAuditFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestAuditFilter.class);

    private String appName;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.appName = filterConfig.getServletContext().getServletContextName();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            log.info("[{}] {} {} took {}ms", appName, http.getMethod(),
                    http.getRequestURI(), System.currentTimeMillis() - start);
        }
    }

    @Override
    public void destroy() {
        // no-op
    }
}
