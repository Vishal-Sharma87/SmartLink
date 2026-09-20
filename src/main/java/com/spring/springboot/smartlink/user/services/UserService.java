package com.spring.springboot.smartlink.user.services;

import com.spring.springboot.smartlink.apiresponse.ResponseMessage;
import com.spring.springboot.smartlink.user.repositories.UserRepository;
import com.spring.springboot.smartlink.link.services.LinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final LinkService linkService;
    private final MongoUserService mongoUserService;
    private final ResponseMessage responseMessage;

    @Transactional
    public String deleteUser(String userEmail) {
        log.info("User account deletion initiated");
        linkService.deleAllLinkOfUser(userEmail);
        userRepository.deleteUserByEmail(userEmail);
        log.info("User account deletion completed");

        return responseMessage.userDeleted();
    }

    public void incrementAndGetMaliciousCount(String email) {
        mongoUserService.incrementAndGetMaliciousUrlCountOfUser(email);
    }

    public boolean existsByEmail(String email) {
        return userRepository.findUserByEmail(email).isPresent();
    }
}
