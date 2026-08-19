package com.spring.springboot.UrlShortener.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.spring.springboot.UrlShortener.dto.virusTotalDtos.AnalysisResultOfVT;
import com.spring.springboot.UrlShortener.entity.LinkScanResponse;
import com.spring.springboot.UrlShortener.services.LinkScanResponseService;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.Instant;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FinalVerdict {

    @ToString
    public enum Verdict {
        SAFE, SUSPICIOUS, UNVERIFIED, MALICIOUS, PENDING_REVERIFICATION;

        @JsonValue
        public String toJson() {
            return this.name();
        }
    }

    private final LinkScanResponseService linkScanResponseService;

    public Verdict evaluate(AnalysisResultOfVT.Stats stats, String hash, String originalUrl) {
        int total = stats.getHarmless() + stats.getMalicious() + stats.getSuspicious()
                + stats.getUndetected() + stats.getTimeout();

        if (total == 0) {
            return Verdict.UNVERIFIED;
        }
        
        Verdict anaylysedVerdict;
        VerdictReason verdictReason;

        double harmlessRatio = (double) stats.getHarmless() / total;
        double undetectedRatio = (double) stats.getUndetected() / total;
        double maliciousRatio = (double) stats.getMalicious() / total;
        double timeoutRatio = (double) stats.getTimeout() / total;

        if (maliciousRatio > 0.05) {
            anaylysedVerdict = Verdict.MALICIOUS;
            verdictReason = VerdictReason.MALICIOUS_THRESHOLD_EXCEEDED;
        } else if (stats.getMalicious() > 0 || stats.getSuspicious() > 0) {
            anaylysedVerdict = Verdict.MALICIOUS;
            verdictReason = VerdictReason.MALICIOUS_THRESHOLD_EXCEEDED;
        } else if (harmlessRatio >= 0.5 && timeoutRatio < 0.2) {
            anaylysedVerdict = Verdict.SAFE;
            verdictReason = VerdictReason.SAFE_CONSENSUS_MET;
        } else if (undetectedRatio > 0.6 || timeoutRatio > 0.4) {
            anaylysedVerdict = Verdict.UNVERIFIED;
            verdictReason = VerdictReason.UNDETECTED_MAJORITY;
        } else {
            anaylysedVerdict = Verdict.SUSPICIOUS;
            verdictReason = VerdictReason.FALLBACK_NO_CONDITION_MET;
        }

        LinkScanResponse linkScanResponse = LinkScanResponse.builder()
        .analysedAt(Instant.now())
        .verdict(anaylysedVerdict)
        .maliciousRatio(maliciousRatio)
        .undetectedRatio(undetectedRatio)
        .timeoutRatio(timeoutRatio)
        .verdictReason(verdictReason)
        .originalUrl(originalUrl)
        .hash(hash)
        .totalEngines(total)
        .harmlessRatio(harmlessRatio)
        .build();

        linkScanResponseService.saveNew(linkScanResponse);                

        return anaylysedVerdict;
    }

}
