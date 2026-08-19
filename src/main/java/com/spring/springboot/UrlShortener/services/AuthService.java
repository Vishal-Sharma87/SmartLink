package com.spring.springboot.UrlShortener.services;

import com.spring.springboot.UrlShortener.advices.exceptions.InvalidOTPException;
import com.spring.springboot.UrlShortener.advices.exceptions.UserWithUserNameAlreadyExitsException;
import com.spring.springboot.UrlShortener.dto.requestDtos.LoginRequestDto;
import com.spring.springboot.UrlShortener.dto.requestDtos.SignupRequestDto;
import com.spring.springboot.UrlShortener.entity.UrlUser;
import com.spring.springboot.UrlShortener.repositories.UserRepository;
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
    public void registerUser(SignupRequestDto request) {
        UrlUser userInDb = userRepository.findUserByUserName(request.getUserName());
        if (userInDb != null)
            throw new UserWithUserNameAlreadyExitsException("User with userName :" + request.getUserName() + " already exists");
        try {
            boolean valid = otpService.isValidOtp(request.getEmail(), request.getOtp());

            if (!valid) throw new InvalidOTPException("Invalid OTP, email: " + request.getEmail());

            UrlUser user = UrlUser.builder()
                    .email(request.getEmail())
                    .userName(request.getUserName())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .roles(List.of("USER"))
                    .creationDate(new Date())
                    .maliciousUrlsCreatedCount(0)
                    .build();

            userRepository.save(user);
//        "User registered successfully!"


        } catch (Exception e) {
            log.error("Something went wrong while validating user OTP for email: {}, exception: {}", request.getEmail(), e.getMessage());
            throw new InvalidOTPException("Invalid otp, Email:" + request.getEmail());
        }
    }

    // Business logic for login
    public String loginUser(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUserName(),
                        request.getPassword()
                )
        );
        return jwtService.generateJwt(request.getUserName());
    }
}
