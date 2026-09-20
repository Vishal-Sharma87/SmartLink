package com.spring.springboot.smartlink.auth;

import com.spring.springboot.smartlink.apiresponse.ApiResponse;
import com.spring.springboot.smartlink.apiresponse.ResponseMessage;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.user.authentication.controllers.AuthController;
import com.spring.springboot.smartlink.user.authentication.dtos.*;
import com.spring.springboot.smartlink.user.authentication.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

@ExtendWith(MockitoExtension.class)
public class SignupTest {

    String linkCreated = "Link created successfully.";
    String linkCreationPending = "Link creation is pending analysis.";
    String allLinksOfUserMessage = "All links retrieved successfully.";
    String linkDeleted = "Link deleted successfully.";
    String allLinksOfUserDeleted = "All links of the user deleted successfully.";
    String userDeleted = "User deleted successfully.";
    String reportAccepted = "Abuse report accepted successfully.";
    String signupInitiated = "Signup initiated successfully. Please verify your email.";
    String malformedSignupSession = "The signup session is malformed or invalid.";
    String authCompleted = "Authentication completed successfully.";
    String refreshTokenRotated = "Refresh token rotated successfully.";

    String linkNotFound = "Link not found for hash: %s";
    String invalidLinkHashFormat = "Invalid link hash format: %s";
    String userNotFound = "User not found: %s";
    String noScanResultFound = "No scan result found for short code: %s";
    String linkAlreadyReported = "This link has already been reported by this email";
    String invalidOrExpiredOTP = "Invalid or expired OTP";
    String atLeastOneFieldRequired = "At least one field (username, email, or password) must be provided for update";
    String sha256Unavailable = "SHA-256 algorithm is unavailable";
    String userWithEmailExists = "User with email already exists: %s";
    String pleaseLoginAgain = "Please login again";
    String requestContainsInvalidFields = "Request contains invalid or missing fields";
    String authenticationFailed = "Authentication failed";
    String accessDenied = "You do not have permission to access this resource";
    String unexpectedServerError = "An unexpected server error occurred";

    private final ExceptionMessages exceptionMessages = new ExceptionMessages(
            linkNotFound,
            invalidLinkHashFormat,
            userNotFound,
            noScanResultFound,
            linkAlreadyReported,
            invalidOrExpiredOTP,
            atLeastOneFieldRequired,
            sha256Unavailable,
            userWithEmailExists,
            pleaseLoginAgain,
            requestContainsInvalidFields,
            authenticationFailed,
            accessDenied,
            unexpectedServerError
    );

    private final ResponseMessage responseMessage = new ResponseMessage(
            linkCreated,
            linkCreationPending,
            allLinksOfUserMessage,
            linkDeleted,
            allLinksOfUserDeleted,
            userDeleted,
            reportAccepted,
            signupInitiated,
            malformedSignupSession,
            authCompleted,
            refreshTokenRotated
    );

    HttpSession httpSession = new MockHttpSession();
    String firstName = "firstName";
    String lastName = "lastName";
    String email = "email@example.com";
    String password = "password";
    String otp = "124516";

    String jwtToken = "with.two.period";
    String refreshToken = "with.two.period";
    @Mock
    private AuthService authService;

    private AuthController authController;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @BeforeEach
    public void setup() {
        this.authController = new AuthController(authService, responseMessage, exceptionMessages);
    }

    @Test
    public void testValidSignupRequest() {
        SignupInitiateDto signupInitiateDto = new SignupInitiateDto(
                firstName,
                lastName,
                email);

        signupInitiateDto.setPassword(password);
        Mockito.when(authService.initiateSignup(signupInitiateDto, httpSession)).thenReturn(new SignupInitiateResponse(email, responseMessage.signupInitiated()));


        ResponseEntity<ApiResponse<SignupInitiateResponse>> apiResponseResponseEntity =
                Assertions.assertDoesNotThrow(
                        () -> authController.initiateSignup(signupInitiateDto, httpSession));

        Assertions.assertEquals(HttpStatus.OK, apiResponseResponseEntity.getStatusCode());
        Assertions.assertNotNull(apiResponseResponseEntity.getBody());
        Assertions.assertNotNull(apiResponseResponseEntity.getBody().data());
        Assertions.assertEquals(email, apiResponseResponseEntity.getBody().data().email());

    }

    @Test
    public void testSignupVerification() {
        OtpDto otpDto = new OtpDto(otp);

        Mockito.when(authService.getJwtIfSignupCompletes(otp, httpSession)).thenReturn(new AuthTokens(
                jwtToken,
                refreshToken
        ));

        ResponseEntity<ApiResponse<AuthResponse>> apiResponseResponseEntity = Assertions.assertDoesNotThrow(
                () -> authController.verifySignup(otpDto, httpSession, httpServletResponse));


        Assertions.assertEquals(HttpStatus.OK, apiResponseResponseEntity.getStatusCode());
        Assertions.assertNotNull(apiResponseResponseEntity.getBody());
        Assertions.assertNotNull(apiResponseResponseEntity.getBody().data());
        Assertions.assertEquals(jwtToken, apiResponseResponseEntity.getBody().data().jwtToken());

    }

}
