package com.spring.springboot.smartlink.advices;

import com.spring.springboot.smartlink.advices.exceptions.*;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AllFieldsNullException.class)
    public ResponseEntity<ApiError> handleAllFieldsNull(AllFieldsNullException ex) {
        log.warn("Request rejected because all update fields were empty");
        ApiError error = new ApiError(ex.getMessage(), 400);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotExistsException.class)
    public ResponseEntity<ApiError> handleResourceNotExists(ResourceNotExistsException ex) {
        log.warn("Requested resource was not found");
        ApiError error = new ApiError(ex.getMessage(), 404);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceWithHashNotExistsException.class)
    public ResponseEntity<ApiError> handleHashNotExists(ResourceWithHashNotExistsException ex) {
        log.warn("Requested resource hash was not found");
        ApiError error = new ApiError(ex.getMessage(), 404);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoSuchLinkExists.class)
    public ResponseEntity<ApiError> handleNoSuchLinkExists(NoSuchLinkExists ex) {
        log.warn("Requested link does not exist");
        ApiError error = new ApiError(ex.getMessage(), 404);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserWithUserNameAlreadyExitsException.class)
    public ResponseEntity<ApiError> handleUserNameAlreadyExists(UserWithUserNameAlreadyExitsException ex) {
        log.warn("User registration rejected because the username already exists");
        ApiError error = new ApiError(ex.getMessage(), 409);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(LinkAlreadyReportedByCurrentEmailOfReporterException.class)
    public ResponseEntity<ApiError> handleLinkAlreadyReported(LinkAlreadyReportedByCurrentEmailOfReporterException ex) {
        log.warn("Abuse report rejected because the link was already reported");
        ApiError error = new ApiError(ex.getMessage(), 409);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidOTPException.class)
    public ResponseEntity<ApiError> handleInvalidOtpException(InvalidOTPException ex) {
        log.warn("Request rejected because OTP validation failed");
        // Updated to 401 Unauthorized to match industry standards
        ApiError error = new ApiError(ex.getMessage(), 401);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(SendgridEmailFailedException.class)
    public ResponseEntity<ApiError> handleSendgridEmailFailed(SendgridEmailFailedException ex) {
        log.error("Notification email delivery failed", ex);
        ApiError error = new ApiError("Failed to send notification email. Please try again later.", 500);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication request failed");
        // Changed from 500 to 401 as this is a security/auth failure
        ApiError error = new ApiError(ex.getMessage(), 401);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // Generic fallback for other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(Exception ex) {
        log.error("Unhandled application exception", ex);
        ApiError error = new ApiError(ex.getMessage(), 500);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
