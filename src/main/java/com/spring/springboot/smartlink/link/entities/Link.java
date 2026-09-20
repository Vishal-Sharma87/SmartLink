package com.spring.springboot.smartlink.link.entities;

import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.report.entities.AbuseReport;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "links")
@Data
@Builder
public class Link {

    @Id
    private final String shortCode;

    private final String originalUrl;

    // owner of link, for analysis purposes
    @Indexed
    private final String ownerEmail;

    private final Instant createdAt;

    private Verdict status;

    private List<AbuseReport> abuseReports;

    // if someone report the link as unsafe ,then we will increment the counter
    // if it hits a predefined threshold then will send a mail to owner to take
    // action on it
    private int reportCount;

    private int clickCount;

}
