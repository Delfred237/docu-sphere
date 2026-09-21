package com.docusphere.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Ajoute un traceId unique à chaque requête HTTP.
 * Ce traceId est propagé dans tous les logs de la requête,
 * facilitant le debugging en production.
 */
@Component
@Order(Integer.MIN_VALUE) // Exécuté en premier
public class MdcFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String USER_ID_KEY = "userId";
    private static final String REQUEST_PATH_KEY = "requestPath";
    private static final String REQUEST_METHOD_KEY = "requestMethod";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Générer ou récupérer le traceId (permet le distributed tracing)
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString().substring(0, 8);
            }

            MDC.put(TRACE_ID_KEY, traceId);
            MDC.put(REQUEST_PATH_KEY, request.getRequestURI());
            MDC.put(REQUEST_METHOD_KEY, request.getMethod());

            // Ajouter le traceId dans la réponse pour le client
            response.setHeader("X-Trace-Id", traceId);

            filterChain.doFilter(request, response);
        } finally {
            // Toujours nettoyer le MDC pour éviter les fuites
            MDC.clear();
        }
    }
}