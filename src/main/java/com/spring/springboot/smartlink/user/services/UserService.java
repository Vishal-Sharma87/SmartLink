package com.spring.springboot.smartlink.user.services;

import com.spring.springboot.smartlink.apiresponse.ResponseMessage;
import com.spring.springboot.smartlink.user.entities.User;
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
    public String deleteUser(String userName) {
        log.info("User deletion requested for username {}", userName);
        linkService.deleAllLinkOfUser(userName);
        userRepository.deleteUserByUserName(userName);
        log.info("User account deleted for username {}", userName);

        return responseMessage.userDeleted();
    }

    public User getUserByUserName(String userName) {
        return userRepository.findUserByUserName(userName);
    }

    public void incrementAndGetMaliciousCount(String userName) {
        mongoUserService.incrementAndGetMaliciousUrlCountOfUser(userName);
    }
}
