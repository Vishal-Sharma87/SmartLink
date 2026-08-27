package com.spring.springboot.smartlink.repositories;

import com.spring.springboot.smartlink.entity.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface UserRepository extends MongoRepository<User, ObjectId> {
    User findUserByUserName(String userName);

    void deleteUserByUserName(String userName);
}
