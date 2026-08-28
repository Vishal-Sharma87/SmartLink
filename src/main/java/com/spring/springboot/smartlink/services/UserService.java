package com.spring.springboot.smartlink.services;

import com.spring.springboot.smartlink.dto.UserCredentialUpdateRequestDto;
import com.spring.springboot.smartlink.entity.User;
import com.spring.springboot.smartlink.advices.exceptions.AllFieldsNullException;
import com.spring.springboot.smartlink.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LinkService linkService;

    public void updateCredentials(UserCredentialUpdateRequestDto dto, String userName) {
        User userInDb = userRepository.findUserByUserName(userName);
        // input validation
        boolean isAllEmpty = (dto.getUserName() == null || dto.getUserName().isBlank()) &&
                (dto.getEmail() == null || dto.getEmail().isBlank()) &&
                (dto.getPassword() == null || dto.getPassword().isBlank());

        if (isAllEmpty) {
            throw new AllFieldsNullException(
                    "At least one field (username, email, or password) must be provided for update");
        }

        String passwordToReplaceWith = dto.getPassword();
        if (passwordToReplaceWith != null && !passwordToReplaceWith.isEmpty()) {
            userInDb.setPassword(passwordEncoder.encode(passwordToReplaceWith));
        }
        // username for update
        String updatedUsername = (dto.getUserName() != null && !dto.getUserName().isBlank())
                ? dto.getUserName()
                : userInDb.getUsername();

        String updatedEmail = (dto.getEmail() != null && !dto.getEmail().isBlank())
                ? dto.getEmail()
                : userInDb.getEmail();

        

        userInDb.setUserName(updatedUsername);
        userInDb.setEmail(updatedEmail);

        userRepository.save(userInDb);
        log.info("User credentials updated for username {}", userName);
    }

    @Transactional
    public void deleteUser(String userName) {
        log.info("User deletion requested for username {}", userName);
        linkService.deleAllLinkOfUser(userName);
        userRepository.deleteUserByUserName(userName);
        log.info("User account deleted for username {}", userName);
    }

    public User getUserByUserName(String userName) {
        return userRepository.findUserByUserName(userName);
    }

    public void save(User user) {
        userRepository.save(user);
        log.debug("User record persisted for username {}", user.getUsername());
    }
}
