package com.spring.springboot.smartlink.user.services;

import com.spring.springboot.smartlink.advices.exceptions.UserNotFoundExceptionSmartLink;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.user.configs.UserKeys;
import com.spring.springboot.smartlink.user.entities.User;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class MongoUserService {

    private final MongoTemplate mongoTemplate;
    private final ExceptionMessages exceptionMessages;
    private final UserKeys userKeys;

    public MongoUserService(
        MongoTemplate mongoTemplate,
        ExceptionMessages exceptionMessages,
        UserKeys userKeys) {
        this.mongoTemplate = mongoTemplate;
        this.exceptionMessages = exceptionMessages;
        this.userKeys = userKeys;
    }

    public void incrementAndGetMaliciousUrlCountOfUser(String email) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where(userKeys.email()).is(email));

        Update update = new Update()
                .inc(userKeys.maliciousUrlsCreatedCount(), 1);

        User updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                User.class);

        if (updated == null)
            throw new UserNotFoundExceptionSmartLink(String.format(exceptionMessages.userNotFound(), email));

    }
}
