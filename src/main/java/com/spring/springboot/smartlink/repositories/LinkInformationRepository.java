package com.spring.springboot.smartlink.repositories;

import com.spring.springboot.smartlink.entity.LinkInformation;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LinkInformationRepository extends MongoRepository<LinkInformation, ObjectId> {

    List<LinkInformation> findAllByAssociatedShortHashOrderByTimeOfClickDesc(String associatedShortHash);
}
