package com.spring.springboot.smartlink.link.repositories;

import com.spring.springboot.smartlink.link.entities.Link;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LinkRepository extends MongoRepository<Link, Long> {
}
