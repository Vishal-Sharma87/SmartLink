package com.spring.springboot.smartlink.user.repositories;

import com.spring.springboot.smartlink.user.entities.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, ObjectId> {

    Optional<User> findUserByEmail(String email);

    void deleteUserByEmail(String email);

}
