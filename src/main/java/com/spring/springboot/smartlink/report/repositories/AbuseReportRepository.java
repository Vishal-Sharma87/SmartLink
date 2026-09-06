package com.spring.springboot.smartlink.report.repositories;

import com.spring.springboot.smartlink.report.entities.AbuseReport;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AbuseReportRepository extends MongoRepository<AbuseReport, ObjectId> {
}
