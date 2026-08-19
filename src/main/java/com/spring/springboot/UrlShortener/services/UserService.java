package com.spring.springboot.UrlShortener.services;

import com.spring.springboot.UrlShortener.dto.UserCredentialUpdateRequestDto;
import com.spring.springboot.UrlShortener.entity.UrlUser;
import com.spring.springboot.UrlShortener.advices.exceptions.AllFieldsNullException;
import com.spring.springboot.UrlShortener.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LinkService linkService;

    public void updateCredentials(UserCredentialUpdateRequestDto dto, String userName) {
        UrlUser userInDb = userRepository.findUserByUserName(userName);
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
    }

    @Transactional
    public void deleteUser(String userName) {
        linkService.deleteAllLinksOfAnUserByType(userName, "all");
        userRepository.deleteUserByUserName(userName);
    }

    public UrlUser getUserByUserName(String userName) {
        return userRepository.findUserByUserName(userName);
    }

    public void save(UrlUser user) {
        userRepository.save(user);
    }
}
