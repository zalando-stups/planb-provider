package org.zalando.planb.provider;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

@Component
class RequestInfoMDCFilter implements Filter {

    private static final String KEY = "request";
    private static final String X_FORWARDED_FOR = "x-forwarded-for";
    private static final String REQUEST_INFO_FORMAT = "%s %s <- %s"; // e.g. "POST /oauth2/access_token <- 127.0.0.1"

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            putMDCRequestInfo(servletRequest);
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.remove(KEY);
        }
    }

    private void putMDCRequestInfo(ServletRequest servletRequest) {
        try {
            Optional.ofNullable(servletRequest)
                    .filter(o -> o instanceof HttpServletRequest)
                    .map(o -> (HttpServletRequest) o)
                    .map(RequestInfoMDCFilter::toRequestInfoString)
                    .ifPresent(RequestInfoMDCFilter::putToMDC);
        } catch (Throwable ignored) {
        }
    }

    private static String toRequestInfoString(HttpServletRequest request) {
        return String.format(REQUEST_INFO_FORMAT, request.getMethod(), request.getRequestURI(), getRemoteAddr(request));
    }

    private static String getRemoteAddr(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(X_FORWARDED_FOR)).orElseGet(request::getRemoteAddr);
    }

    private static void putToMDC(String requestInfo) {
        MDC.put(KEY, requestInfo);
    }
}
