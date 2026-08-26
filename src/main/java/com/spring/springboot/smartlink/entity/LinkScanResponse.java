package com.spring.springboot.smartlink.entity;

import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.enums.VerdictReason;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "link_scan_matrices")
public class LinkScanResponse {

    @Id
    private String hash;
    private String originalUrl;
    private Verdict verdict;
    private VerdictReason verdictReason;
    private double harmlessRatio;
    private double maliciousRatio;
    private double undetectedRatio;
    private double timeoutRatio;
    private int totalEngines;

    private Instant analysedAt;
}