package com.spring.springboot.UrlShortener.advices;

import com.spring.springboot.UrlShortener.advices.exceptions.*;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AllFieldsNullException.class)
    public ResponseEntity<ApiError> handleAllFieldsNull(AllFieldsNullException ex) {
        ApiError error = new ApiError(ex.getMessage(), 400);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotExistsException.class)
    public ResponseEntity<ApiError> handleResourceNotExists(ResourceNotExistsException ex) {
        ApiError error = new ApiError(ex.getMessage(), 404);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceWithHashNotExistsException.class)
    public ResponseEntity<ApiError> handleHashNotExists(ResourceWithHashNotExistsException ex) {
        ApiError error = new ApiError(ex.getMessage(), 404);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoSuchLinkExists.class)
    public ResponseEntity<ApiError> handleNoSuchLinkExists(NoSuchLinkExists ex) {
        ApiError error = new ApiError(ex.getMessage(), 404);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserWithUserNameAlreadyExitsException.class)
    public ResponseEntity<ApiError> handleUserNameAlreadyExists(UserWithUserNameAlreadyExitsException ex) {
        ApiError error = new ApiError(ex.getMessage(), 409);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(LinkAlreadyReportedByCurrentEmailOfReporterException.class)
    public ResponseEntity<ApiError> handleLinkAlreadyReported(LinkAlreadyReportedByCurrentEmailOfReporterException ex) {
        ApiError error = new ApiError(ex.getMessage(), 409);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidOTPException.class)
    public ResponseEntity<ApiError> handleInvalidOtpException(InvalidOTPException ex) {
        // Updated to 401 Unauthorized to match industry standards
        ApiError error = new ApiError(ex.getMessage(), 401);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(SendgridEmailFailedException.class)
    public ResponseEntity<ApiError> handleSendgridEmailFailed(SendgridEmailFailedException ex) {
        ApiError error = new ApiError("Failed to send notification email. Please try again later.", 500);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
        // Changed from 500 to 401 as this is a security/auth failure
        ApiError error = new ApiError(ex.getLocalizedMessage(), 401);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // Generic fallback for other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(Exception ex) {
        ApiError error = new ApiError(ex.getLocalizedMessage(), 500);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}