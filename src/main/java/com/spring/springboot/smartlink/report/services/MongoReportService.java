package com.spring.springboot.smartlink.report.services;

import com.spring.springboot.smartlink.link.services.MongoLinkService;
import com.spring.springboot.smartlink.report.dtos.ReportLinkRequestDto;
import com.spring.springboot.smartlink.report.entities.AbuseReport;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MongoReportService {

    private final MongoTemplate mongoTemplate;
    private final MongoLinkService mongoLinkService;

    public MongoReportService(
            MongoTemplate mongoTemplate, MongoLinkService mongoLinkService) {

        this.mongoTemplate = mongoTemplate;
        this.mongoLinkService = mongoLinkService;
    }

    public void insert(AbuseReport report) {
        mongoTemplate.insert(report);
    }

    @Transactional
    public int acceptReportAndGetReportCount(ReportLinkRequestDto dto) {
        String[] parts = dto.linkToReport().split("/");
        String shortCode = parts[parts.length - 1];

//        1. Create Entity
        AbuseReport report = AbuseReport.builder()
                .reporterName(dto.reporterName())
                .reporterEmail(dto.reporterEmail())
                .reportStatus("SUCCESSFUL")
                .cause(dto.cause())
                .description(dto.description())
                .shortCode(shortCode)
                .createdAt(LocalDateTime.now())
                .build();

        // 2. Insert report
        insert(report);

        // 3. Atomically increment reportCount
        return mongoLinkService.incrementAndGetReportCount(shortCode);
    }
}
