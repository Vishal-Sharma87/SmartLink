package com.spring.springboot.smartlink.virustotal.services;

import com.spring.springboot.smartlink.virustotal.dto.AnalysisResultOfVT;
import com.spring.springboot.smartlink.analytics.entities.LinkScanResponse;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.enums.VerdictReason;

import com.spring.springboot.smartlink.analytics.services.LinkScanResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerdictEvaluationService {

    private final LinkScanResponseService linkScanResponseService;

    public Verdict evaluate(AnalysisResultOfVT.Stats stats, String hash, String originalUrl) {
        int total = stats.getHarmless() + stats.getMalicious() + stats.getSuspicious()
                + stats.getUndetected() + stats.getTimeout();

        if (total == 0) {
            log.warn("No analysis engines returned signals for short code {}; marking as {}", hash, Verdict.UNVERIFIED);
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
        log.info("URL analysis completed for short code {} with verdict {} and reason {}", hash, anaylysedVerdict,
                verdictReason);

        return anaylysedVerdict;
    }

}
