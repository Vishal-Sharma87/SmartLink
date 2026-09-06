package com.spring.springboot.smartlink.analytics.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spring.springboot.smartlink.analytics.entities.LinkScanResponse;

public interface LinkScanResponseRepository extends MongoRepository<LinkScanResponse, String> {

}
