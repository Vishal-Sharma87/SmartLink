package com.spring.springboot.smartlink.repositories;

import com.spring.springboot.smartlink.entity.AbuseReport;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AbuseReportRepository extends MongoRepository<AbuseReport, ObjectId> {
}
