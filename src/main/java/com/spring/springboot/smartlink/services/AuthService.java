package com.spring.springboot.smartlink.services;

import com.spring.springboot.smartlink.advices.exceptions.InvalidOTPException;
import com.spring.springboot.smartlink.advices.exceptions.UserWithUserNameAlreadyExitsException;
import com.spring.springboot.smartlink.dto.requestDtos.LoginRequestDto;
import com.spring.springboot.smartlink.dto.requestDtos.SignupRequestDto;
import com.spring.springboot.smartlink.dto.requestDtos.SignupInitiationRequestDto;
import com.spring.springboot.smartlink.dto.requestDtos.SignupVerificationRequestDto;
import com.spring.springboot.smartlink.entity.User;
import com.spring.springboot.smartlink.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpService otpService;

    // Business logic for signup
    public void initiateSignup(SignupInitiationRequestDto request) {
        if (userRepository.findUserByUserName(request.getUserName()) != null) {
            throw new UserWithUserNameAlreadyExitsException("User with userName :" + request.getUserName() + " already exists");
        }
        otpService.sendOtp(request.getEmail());
        log.info("Signup OTP generated for email {}", request.getEmail());
    }

    public void completeSignup(SignupInitiationRequestDto details, SignupVerificationRequestDto verification) {
        if (userRepository.findUserByUserName(details.getUserName()) != null) {
            throw new UserWithUserNameAlreadyExitsException("User with userName :" + details.getUserName() + " already exists");
        }
        if (!otpService.isValidOtp(details.getEmail(), verification.getOtp())) {
            throw new InvalidOTPException("Invalid OTP, email: " + details.getEmail());
        }
        saveUser(details);
        log.info("User registration completed for username {}", details.getUserName());
    }

    public void registerUser(SignupRequestDto request) {
        User userInDb = userRepository.findUserByUserName(request.getUserName());
        if (userInDb != null)
            throw new UserWithUserNameAlreadyExitsException("User with userName :" + request.getUserName() + " already exists");
        try {
            boolean valid = otpService.isValidOtp(request.getEmail(), request.getOtp());

            if (!valid) throw new InvalidOTPException("Invalid OTP, email: " + request.getEmail());

            SignupInitiationRequestDto details = new SignupInitiationRequestDto();
            details.setEmail(request.getEmail());
            details.setUserName(request.getUserName());
            details.setPassword(request.getPassword());
            saveUser(details);
            log.info("User registration completed for username {}", request.getUserName());
//        "User registered successfully!"


        } catch (Exception e) {
            log.error("Something went wrong while validating user OTP for email: {}, exception: {}", request.getEmail(), e.getMessage());
            log.error("User registration failed for username {}", request.getUserName(), e);
            throw new InvalidOTPException("Invalid otp, Email:" + request.getEmail());
        }
    }

    private void saveUser(SignupInitiationRequestDto request) {
        userRepository.save(User.builder()
                .email(request.getEmail())
                .userName(request.getUserName())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(List.of("USER"))
                .creationDate(new Date())
                .maliciousUrlsCreatedCount(0)
                .build());
    }

    // Business logic for login
    public String loginUser(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUserName(),
                        request.getPassword()
                )
        );
        log.info("User authentication succeeded for username {}", request.getUserName());
        return jwtService.generateJwt(request.getUserName());
    }
}
