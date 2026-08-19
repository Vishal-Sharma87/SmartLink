package com.spring.springboot.UrlShortener.controllers;

import com.spring.springboot.UrlShortener.dto.UserCredentialUpdateRequestDto;
import com.spring.springboot.UrlShortener.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/update-user-credentials")
    public ResponseEntity<Void> updateUserCredentials(
            @RequestBody UserCredentialUpdateRequestDto user
    ) {
        try {
            String userName =
                    SecurityContextHolder.getContext().getAuthentication().getName();
            userService.updateCredentials(user, userName);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete-user")
    public ResponseEntity<Void> deleteUser() {
        try {
            String userName =
                    SecurityContextHolder.getContext().getAuthentication().getName();
            userService.deleteUser(userName);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
