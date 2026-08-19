package com.spring.springboot.UrlShortener.services;

import com.spring.springboot.UrlShortener.entity.UrlUser;
import com.spring.springboot.UrlShortener.advices.exceptions.ResourceNotExistsException;
import com.spring.springboot.UrlShortener.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UrlUser userInDb = userRepository.findUserByUserName(username);

        if (userInDb == null)
            throw new ResourceNotExistsException("User with userName " + username + " Not exists in database");

        return User.builder()
                .authorities(userInDb.getAuthorities())
                .username(userInDb.getUsername())
                .password(userInDb.getPassword())
                .build();
    }
}
