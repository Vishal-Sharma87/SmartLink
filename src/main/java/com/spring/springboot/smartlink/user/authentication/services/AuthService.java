package com.spring.springboot.smartlink.user.authentication.services;

import com.spring.springboot.smartlink.advices.exceptions.InvalidOTPExceptionSmartLink;
import com.spring.springboot.smartlink.advices.exceptions.UsernameAlreadyExistsExceptionSmartLink;
import com.spring.springboot.smartlink.apiresponse.ResponseMessage;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.user.authentication.dtos.*;
import com.spring.springboot.smartlink.email.services.EmailService;
import com.spring.springboot.smartlink.user.entities.User;
import com.spring.springboot.smartlink.jwt.services.JwtService;
import com.spring.springboot.smartlink.otp.services.OtpService;
import com.spring.springboot.smartlink.user.repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
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

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            OtpService otpService,
            ExceptionMessages exceptionMessages,
            EmailService emailService,
            ResponseMessage responseMessage) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.exceptionMessages = exceptionMessages;
        this.emailService = emailService;
        this.responseMessage = responseMessage;

    }

    // Business logic for signup
    public SignupInitiateResponse initiateSignup(SignupInitiationRequestDto request, HttpSession session) {
        if (userRepository.findUserByUserName(request.getUserName()) != null) {
            throw new UsernameAlreadyExistsExceptionSmartLink(
                    String.format(exceptionMessages.usernameAlreadyExists(), request.getUserName()));
        }
        session.setAttribute(PENDING_SIGNUP_SESSION_KEY, request);
        otpService.sendOtp(request.getEmail());

        // replace password from raw string to encoded to avoid plain strings
        request.setPassword(passwordEncoder.encode(request.getPassword()));

        log.info("Signup OTP generated for email {}", request.getEmail());

        return new SignupInitiateResponse(request.getEmail(), responseMessage.signupInitiated());
    }

    public AuthResponse getJwtIfSignupCompletes(
            SignupVerificationRequestDto verification,
            HttpSession session) {
        SignupInitiationRequestDto pending = (SignupInitiationRequestDto) session
                .getAttribute(PENDING_SIGNUP_SESSION_KEY);

        if (pending == null) {
            return AuthResponse.of(null, responseMessage.malformedSignupSession());
        }

        String userName = pending.getUserName();
        String email = pending.getEmail();

        if (userRepository.findUserByUserName(userName) != null) {
            throw new UsernameAlreadyExistsExceptionSmartLink(
                    String.format(exceptionMessages.usernameAlreadyExists(), userName));
        }
        if (otpService.isInvalidOtp(email, verification.getOtp())) {
            throw new InvalidOTPExceptionSmartLink(exceptionMessages.invalidOtp());
        }

        saveUser(pending);
        session.removeAttribute(PENDING_SIGNUP_SESSION_KEY);

        emailService.sendWelcomeEmail(userName, email);

        log.info("User registration completed for username {}", userName);

        return buildAuthResponse(userName);
    }

    public AuthResponse registerUser(SignupRequestDto request) {
        User userInDb = userRepository.findUserByUserName(request.getUserName());

        if (userInDb != null)
            throw new UsernameAlreadyExistsExceptionSmartLink(
                    String.format(exceptionMessages.usernameAlreadyExists(), request.getUserName()));

        if (otpService.isInvalidOtp(request.getEmail(), request.getOtp()))
            throw new InvalidOTPExceptionSmartLink(exceptionMessages.invalidOtp());

        SignupInitiationRequestDto details = new SignupInitiationRequestDto();
        details.setEmail(request.getEmail());
        details.setUserName(request.getUserName());
        details.setPassword(passwordEncoder.encode(request.getPassword()));

        saveUser(details);
        emailService.sendWelcomeEmail(details.getUserName(), details.getEmail());

        return buildAuthResponse(request.getUserName());
    }

    private void saveUser(SignupInitiationRequestDto request) {
        userRepository.save(User.builder()
                .email(request.getEmail())
                .userName(request.getUserName())
                .password(request.getPassword())
                .roles(List.of("USER"))
                .creationDate(new Date())
                .maliciousUrlsCreatedCount(0)
                .build());
    }

    public AuthResponse loginUser(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUserName(),
                        request.getPassword()));
        log.info("User authentication succeeded for username {}", request.getUserName());

        return buildAuthResponse(request.getUserName());
    }

    private AuthResponse buildAuthResponse(String userName) {
        String token = jwtService.generateJwt(userName);
        return AuthResponse.of(token, responseMessage.authCompleted());
    }
}
