package com.spring.springboot.UrlShortener.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spring.springboot.UrlShortener.entity.LinkScanResponse;

public interface LinkScanresponseRepository extends MongoRepository<LinkScanResponse, String> {

}
