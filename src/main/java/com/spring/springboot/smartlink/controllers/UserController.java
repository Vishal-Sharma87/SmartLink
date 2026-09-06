package com.spring.springboot.smartlink.controllers;

import com.spring.springboot.smartlink.services.UserService;
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

    @GetMapping({"", "/"})
    public ResponseEntity<String> health(){
        return ResponseEntity.ok("Working");
    }

    @DeleteMapping("/delete-user")
    public ResponseEntity<Void> deleteUser() {
        try {
            String userName =
                    SecurityContextHolder.getContext().getAuthentication().getName();
            userService.deleteUser(userName);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to delete user account", e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
