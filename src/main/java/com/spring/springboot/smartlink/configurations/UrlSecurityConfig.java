package com.spring.springboot.smartlink.configurations;

import com.spring.springboot.smartlink.filter.JwtFilter;
import com.spring.springboot.smartlink.services.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class UrlSecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http


                // Authorize requests
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new RegexRequestMatcher("^/[a-zA-Z0-9]{7}$", "GET")
                        ).permitAll()
                        .requestMatchers("/user", "/user/**").hasRole("USER")
                        .requestMatchers(
                        "/auth/**"
                                , "/"
                                , "/home"
                                , "/about"
                                , "/auth"
                                , "/login"
                                , "/signup"
                                , "/signup/verify"
                                , "/dashboard"
                                , "/analytics"
                                , "/report-abuse-page"
                                , "/report"
                                , "/css/**"
                                , "/js/**"
                                , "/favicon.ico"
                                , "/verify/**"
                                , "/report-abuse/**"
                                , "/api/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // Enable JWT based Authentication
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // Disable CSRF for simplicity (only if APIs, not for forms)
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(getPasswordEncoder());
        return provider;
    }

    // Authentication manager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


}
