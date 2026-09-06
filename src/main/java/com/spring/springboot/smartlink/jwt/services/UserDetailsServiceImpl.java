package com.spring.springboot.smartlink.jwt.services;

import com.spring.springboot.smartlink.user.entities.User;
import com.spring.springboot.smartlink.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User userInDb = userRepository.findUserByUserName(username);

        if (userInDb == null) {
            log.warn("Authentication rejected because username {} does not exist", username);
            throw new UsernameNotFoundException(username);
        }

        log.debug("User details loaded for username {}", username);

        return org.springframework.security.core.userdetails.User.builder()
                .authorities(userInDb.getAuthorities())
                .username(userInDb.getUsername())
                .password(userInDb.getPassword())
                .build();
    }
}
