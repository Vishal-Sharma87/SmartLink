package com.spring.springboot.smartlink.jwt.filter;

import com.spring.springboot.smartlink.advices.enums.ErrorCode;
import com.spring.springboot.smartlink.advices.exceptions.SmartLinkApplicationException;
import com.spring.springboot.smartlink.advices.exceptions.SmartlinkAuthException;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.jwt.services.UserDetailsServiceImpl;
import com.spring.springboot.smartlink.jwt.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final ExceptionMessages exceptionMessages;

    public JwtFilter(
            JwtService jwtService,
            UserDetailsServiceImpl userDetailsService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver,
            ExceptionMessages exceptionMessages) {

        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
        this.exceptionMessages = exceptionMessages;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String extractedHeader = request.getHeader("Authorization");

            if (extractedHeader == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!extractedHeader.startsWith("Bearer ") || extractedHeader.length() <= 7) {
                throw authenticationFailure();
            }

            String jwtToken = extractedHeader.substring(7);

            String email = jwtService.getSubjectFromJwtToken(jwtToken);

            if (email == null || email.isBlank()) {
                throw authenticationFailure();
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                // propagate authorities so role checks (e.g., hasRole("USER")) work
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());

                auth.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT authentication completed for request {} {}", request.getMethod(), request.getRequestURI());
            }

        } catch (Exception e) {
            log.warn("JWT authentication failed for request {} {}", request.getMethod(), request.getRequestURI());
            Exception safeException = e instanceof SmartLinkApplicationException
                    ? e
                    : authenticationFailure();
            handlerExceptionResolver.resolveException(request, response, null, safeException);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private SmartlinkAuthException authenticationFailure() {
        return new SmartlinkAuthException(ErrorCode.AUTHENTICATION_FAILED,
                exceptionMessages.authenticationFailed());
    }
}
