package com.spring.springboot.smartlink.advices.handler;

import com.spring.springboot.smartlink.advices.dtos.ApiError;
import com.spring.springboot.smartlink.advices.enums.ErrorCode;
import com.spring.springboot.smartlink.advices.exceptions.SmartLinkApplicationException;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final ExceptionMessages exceptionMessages;

    public GlobalExceptionHandler(ExceptionMessages exceptionMessages) {
        this.exceptionMessages = exceptionMessages;
    }

    @ExceptionHandler(SmartLinkApplicationException.class)
    public ResponseEntity<ApiError> handleApplicationException(SmartLinkApplicationException ex) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus())
                .body(new ApiError(LocalDateTime.now(), ex.getMessage(), code.name(), code.getHttpStatus().value()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication request failed");
        return errorResponse(ErrorCode.AUTHENTICATION_FAILED, exceptionMessages.authenticationFailed());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access to a protected resource was denied");
        return errorResponse(ErrorCode.ACCESS_DENIED, exceptionMessages.accessDenied());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class,
            ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleInvalidRequest(Exception ex) {
        log.debug("Request rejected by input validation or deserialization", ex);
        return errorResponse(ErrorCode.INVALID_REQUEST, exceptionMessages.validationException());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(Exception ex) {
        log.error("Unhandled application exception", ex);
        return errorResponse(ErrorCode.INTERNAL_SERVER_ERROR, exceptionMessages.internalServerError());
    }

    private ResponseEntity<ApiError> errorResponse(ErrorCode code, String message) {
        ApiError error = new ApiError(LocalDateTime.now(), message, code.name(), code.getHttpStatus().value());
        return ResponseEntity.status(code.getHttpStatus()).body(error);
    }
}
