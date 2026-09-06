package com.spring.springboot.smartlink.advices;

import com.spring.springboot.smartlink.advices.exceptions.SmartLinkApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SmartLinkApplicationException.class)
    public ResponseEntity<ApiError> handleApplicationException(SmartLinkApplicationException ex) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus())
                .body(new ApiError(LocalDateTime.now(), ex.getMessage(), code.name(), code.getHttpStatus().value()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication request failed");
        ApiError error = new ApiError(LocalDateTime.now(), ex.getMessage(), "AUTHENTICATION_FAILED", 401);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.warn("Authentication request failed because the username was not found");
        ApiError error = new ApiError(LocalDateTime.now(), ex.getMessage(), "USERNAME_NOT_FOUND", 401);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(Exception ex) {
        log.error("Unhandled application exception", ex);
        ApiError error = new ApiError(LocalDateTime.now(), ex.getMessage(), "INTERNAL_SERVER_ERROR", 500);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
