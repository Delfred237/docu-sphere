package com.docusphere.auth.security;

import com.docusphere.auth.repository.UserRepository;
import com.docusphere.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Utilisation de la méthode avec JOIN FETCH
                userRepository.findByEmailIgnoreCaseWithRolesAndPermissions(userEmail).ifPresent(user -> {
                    if (jwtService.isTokenValid(jwt, user) && user.isActive()) {

                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                        // 1. Ajouter les Rôles (ex: ROLE_ADMIN)
                        user.getRoles().forEach(role -> {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().name()));

                            // 2. Ajouter les Permissions de chaque rôle (ex: USER_MANAGE)
                            role.getPermissions().forEach(permission -> {
                                authorities.add(new SimpleGrantedAuthority(permission.getName().name()));
                            });
                        });

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user, null, authorities);
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                });
            }
        } catch (Exception e) {
            // Si le token est invalide/expiré, on ne fait rien.
            // Spring Security renverra un 401/403 plus tard car le SecurityContext est vide.
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
