package com.spring.springboot.smartlink.user.controllers;

import com.spring.springboot.smartlink.apiresponse.ApiResponse;
import com.spring.springboot.smartlink.user.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping({ "", "/" })
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Working");
    }

    @DeleteMapping("/delete-user")
    public ResponseEntity<ApiResponse<String>> deleteUser() {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        String message = userService.deleteUser(userName);
        return ResponseEntity
                .ok(ApiResponse.of(message));
    }
}
