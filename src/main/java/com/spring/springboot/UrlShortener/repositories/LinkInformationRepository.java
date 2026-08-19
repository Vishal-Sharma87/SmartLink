package com.spring.springboot.UrlShortener.repositories;

import com.spring.springboot.UrlShortener.entity.LinkInformation;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LinkInformationRepository extends MongoRepository<LinkInformation, ObjectId> {
}
