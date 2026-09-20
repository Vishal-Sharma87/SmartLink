package com.spring.springboot.smartlink.user.authentication.services;

import com.spring.springboot.smartlink.advices.enums.ErrorCode;
import com.spring.springboot.smartlink.advices.exceptions.InvalidOTPExceptionSmartLink;
import com.spring.springboot.smartlink.advices.exceptions.SmartlinkAuthException;
import com.spring.springboot.smartlink.advices.exceptions.UserWithEmailExistsException;
import com.spring.springboot.smartlink.apiresponse.ResponseMessage;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.user.authentication.dtos.*;
import com.spring.springboot.smartlink.email.services.EmailService;
import com.spring.springboot.smartlink.user.entities.User;
import com.spring.springboot.smartlink.jwt.services.JwtService;
import com.spring.springboot.smartlink.otp.services.OtpService;
import com.spring.springboot.smartlink.user.repositories.UserRepository;
import com.spring.springboot.smartlink.user.services.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class AuthService {
    public static final String PENDING_SIGNUP_SESSION_KEY = "pendingSignup";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final ExceptionMessages exceptionMessages;
    private final EmailService emailService;
    private final ResponseMessage responseMessage;
    private final UserService userService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            OtpService otpService,
            ExceptionMessages exceptionMessages,
            EmailService emailService,
            ResponseMessage responseMessage,
            UserService userService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.exceptionMessages = exceptionMessages;
        this.emailService = emailService;
        this.responseMessage = responseMessage;
        this.userService = userService;
    }

    public SignupInitiateResponse initiateSignup(
            @Valid SignupInitiateDto request,
            HttpSession session) {

        String email = request.getEmail();
        if (userService.existsByEmail(email)) {
            log.warn("Signup initiation rejected because the email is already registered");
            throw new UserWithEmailExistsException(
                    String.format(exceptionMessages.userWithEmailExists(), email));
        }

        // replace password from raw string to encoded to avoid plain strings
        request.setPassword(passwordEncoder.encode(request.getPassword()));

        session.setAttribute(PENDING_SIGNUP_SESSION_KEY, request);
        otpService.sendOtp(email);

        log.info("Signup initiation completed and OTP dispatch requested");

        return new SignupInitiateResponse(request.getEmail(), responseMessage.signupInitiated());
    }

    public AuthTokens getJwtIfSignupCompletes(
            @NotBlank String otp,
            HttpSession session) {

        Object pendingSignup = session.getAttribute(PENDING_SIGNUP_SESSION_KEY);

        if (!(pendingSignup instanceof SignupInitiateDto pending)) {
            log.warn("Signup OTP verification rejected because the pending signup session was unavailable");
            throw new SmartlinkAuthException(ErrorCode.PENDING_SIGNUP_NOT_FOUND, exceptionMessages.authException());
        }

        String email = pending.getEmail();

        if (userService.existsByEmail(email)) {
            log.warn("Signup OTP verification rejected because the email is already registered");
            throw new UserWithEmailExistsException(
                    String.format(exceptionMessages.userWithEmailExists(), email));
        }

        if (otpService.isInvalidOtp(email, otp)) {
            throw new InvalidOTPExceptionSmartLink(exceptionMessages.invalidOtp());
        }

        saveUser(pending);
        session.removeAttribute(PENDING_SIGNUP_SESSION_KEY);

        emailService.sendWelcomeEmail(pending.getFirstName(), email);

        log.info("Signup OTP verification completed and account creation finished");
        return buildAuthResponse(pending.getEmail());
    }

    private void saveUser(
            SignupInitiateDto pending) {

        User userToSave = User.builder()
                .firstName(pending.getFirstName())
                .lastName(pending.getLastName())
                .email(pending.getEmail())
                .password(pending.getPassword())
                .roles(List.of("USER"))
                .creationDate(Instant.now())
                .build();

        userRepository.save(userToSave);
    }

    public AuthTokens loginUser(@Valid LoginDto request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException ex) {
            log.warn("Login rejected for an invalid authentication attempt");
            throw new SmartlinkAuthException(ErrorCode.AUTHENTICATION_FAILED,
                    exceptionMessages.authenticationFailed());
        }
        log.info("Login authentication completed successfully");
        return buildAuthResponse(request.email());
    }

    private AuthTokens buildAuthResponse(String email) {
        return jwtService.generateJwt(email);
    }

    public AuthTokens refreshToken(String refreshToken) {
        AuthTokens tokens = jwtService.rotateTokensIfValid(refreshToken);
        log.info("Refresh token rotation completed successfully");
        return tokens;
    }
}
