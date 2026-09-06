package com.spring.springboot.smartlink.report.services;

import com.spring.springboot.smartlink.link.configs.LinkKeys;
import com.spring.springboot.smartlink.report.configs.AbuseReportKeys;
import com.spring.springboot.smartlink.report.dtos.ReportLinkRequestDto;
import com.spring.springboot.smartlink.report.entities.AbuseReport;
import jakarta.validation.Valid;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class MongoReportService {

    private final AbuseReportKeys abuseReportKeys;
    private final LinkKeys linkKeys;
    private final MongoTemplate mongoTemplate;

    public MongoReportService(
            AbuseReportKeys abuseReportKeys,
            LinkKeys linkKeys,
            MongoTemplate mongoTemplate) {

        this.abuseReportKeys = abuseReportKeys;
        this.linkKeys = linkKeys;
        this.mongoTemplate = mongoTemplate;
    }

    public boolean isAlreadyReported(String hashedKey, @Valid ReportLinkRequestDto dto) {
        Query query = new Query();
        Criteria criteria = new Criteria().andOperator(
                Criteria.where(linkKeys.hashedKey()).is(hashedKey),
                Criteria.where(abuseReportKeys.reporterEmail()).is(dto.getReporterEmail())

        );

        query.addCriteria(criteria);

        // we will stop finding in db when we find first report entry
        query.limit(1);

        return !mongoTemplate.find(query, AbuseReport.class).isEmpty();
    }
}
