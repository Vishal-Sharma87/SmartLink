package com.spring.springboot.smartlink.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spring.springboot.smartlink.entity.LinkScanResponse;

public interface LinkScanResponseRepository extends MongoRepository<LinkScanResponse, String> {

}
