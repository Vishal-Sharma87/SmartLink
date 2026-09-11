package com.spring.springboot.smartlink.virustotal.services;

import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.virustotal.configs.VirusTotalConfigs;
import com.spring.springboot.smartlink.virustotal.dto.AnalysisIdOfVT;
import com.spring.springboot.smartlink.virustotal.dto.AnalysisResultOfVT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
public class VirusTotalService {

    private final WebClient webClient;
    private final VerdictEvaluationService verdictEvaluationService;
    private final VirusTotalConfigs virusTotalConfigs;

    public VirusTotalService(
            WebClient webClient,
            VirusTotalConfigs virusTotalConfigs,
            VerdictEvaluationService verdictEvaluationService) {

        this.webClient = webClient;
        this.virusTotalConfigs = virusTotalConfigs;
        this.verdictEvaluationService = verdictEvaluationService;
    }

    /**
     * Scans a URL and returns the final verdict asynchronously.
     */
    public Mono<Verdict> scanUrl(String url, String hash) {
        log.debug("Starting URL safety scan for short code {}", hash);

        return submitUrlForAnalysis(url)
                .flatMap(this::pollUntilCompleted)
                .map(result -> evaluateVerdict(result, hash, url))
                .doOnError(e -> log.error("VirusTotal scan failed for short code {}",hash,e))
                .onErrorResume(e -> Mono.just(Verdict.UNVERIFIED));
    }

    /**
     * Submits the URL to VirusTotal and returns the analysis identifier.
     */
    private Mono<AnalysisIdOfVT> submitUrlForAnalysis(String url) {
        return webClient.post()
                .uri(virusTotalConfigs.apiUrl())
                .header("x-apikey", virusTotalConfigs.apiKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("url", url))
                .retrieve()
                .bodyToMono(AnalysisIdOfVT.class);
    }

    /**
     * Polls VirusTotal until the analysis is completed.
     */
    private Mono<AnalysisResultOfVT> pollUntilCompleted(
            AnalysisIdOfVT analysis) {

        String analysisUri = analysis.getData().getLinks().getSelf();

        return Mono.defer(() -> fetchAnalysis(analysisUri))
                .repeatWhenEmpty(repeat ->
                        repeat.delayElements(Duration.ofSeconds(virusTotalConfigs.pollingIntervalSeconds())))
                .timeout(Duration.ofMinutes(virusTotalConfigs.pollingTimeoutMinutes()));
    }

    /**
     * Fetches an analysis result and emits it only when completed.
     */
    private Mono<AnalysisResultOfVT> fetchAnalysis(String analysisUri) {
        return webClient.get()
                .uri(analysisUri)
                .header("x-apikey", virusTotalConfigs.apiKey())
                .retrieve()
                .bodyToMono(AnalysisResultOfVT.class)
                .filter(result ->
                        "completed".equalsIgnoreCase(
                                result.getData()
                                        .getAttributes()
                                        .getStatus()));
    }

    /**
     * Converts VirusTotal analysis statistics into the application's verdict.
     */
    private Verdict evaluateVerdict(
            AnalysisResultOfVT result,
            String hash,
            String url) {

        AnalysisResultOfVT.Stats stats =
                result.getData()
                        .getAttributes()
                        .getStats();

        return verdictEvaluationService.evaluate(stats, hash, url);
    }
}