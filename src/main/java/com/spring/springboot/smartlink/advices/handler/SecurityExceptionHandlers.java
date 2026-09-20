package com.spring.springboot.smartlink.advices.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.springboot.smartlink.advices.dtos.ApiError;
import com.spring.springboot.smartlink.advices.enums.ErrorCode;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class SecurityExceptionHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ExceptionMessages exceptionMessages;

    public SecurityExceptionHandlers(ObjectMapper objectMapper, ExceptionMessages exceptionMessages) {
        this.objectMapper = objectMapper;
        this.exceptionMessages = exceptionMessages;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, ErrorCode.AUTHENTICATION_FAILED, exceptionMessages.authenticationFailed());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, ErrorCode.ACCESS_DENIED, exceptionMessages.accessDenied());
    }

    private void write(HttpServletResponse response, ErrorCode code, String message) throws IOException {
        response.setStatus(code.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ApiError(LocalDateTime.now(), message, code.name(), code.getHttpStatus().value()));
    }
}
