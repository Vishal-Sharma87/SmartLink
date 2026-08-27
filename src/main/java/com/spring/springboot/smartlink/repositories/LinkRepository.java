package com.spring.springboot.smartlink.repositories;

import com.spring.springboot.smartlink.entity.Link;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LinkRepository extends MongoRepository<Link, Long> {
}
