package com.spring.springboot.smartlink.virustotal.services;

import com.spring.springboot.smartlink.virustotal.dto.AnalysisIdOfVT;
import com.spring.springboot.smartlink.virustotal.dto.AnalysisResultOfVT;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.virustotal.configs.VirusTotalConfigs;
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
     * Scans a URL and returns a FinalVerdict asynchronously.
     */
    public Mono<Verdict> scanUrl(String url, String hash) {
        log.debug("Starting URL safety scan for short code {}", hash);

        return webClient.post()
                .uri(virusTotalConfigs.apiUrl())
                .header("x-apikey", virusTotalConfigs.apiKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("url", url))
                .retrieve()
                .bodyToMono(AnalysisIdOfVT.class)
                .flatMap(response -> {
                    String nextUri = response.getData().getLinks().getSelf();

                    return Mono.defer(() -> fetchAnalysis(nextUri))
                            .repeatWhenEmpty(repeat -> repeat.delayElements(Duration.ofSeconds(5)))
                            .timeout(Duration.ofMinutes(2)) // Max polling duration
                            .map(result -> {
                                AnalysisResultOfVT.Stats stats = result.getData().getAttributes().getStats();
                                return verdictEvaluationService.evaluate(stats, hash, url);
                            });
                })
                .doOnError(e -> log.error("VirusTotal scan failed for short code {}", hash, e))
                .onErrorResume(e -> Mono.just(Verdict.UNVERIFIED));

    }

    /**
     * Helper method to fetch analysis and only emit if status == "completed".
     */
    private Mono<AnalysisResultOfVT> fetchAnalysis(String nextUri) {
        return webClient.get()
                .uri(nextUri)
                .header("x-apikey", virusTotalConfigs.apiKey())
                .retrieve()
                .bodyToMono(AnalysisResultOfVT.class)
                // Only emit the result when scan is fully completed
                .filter(result -> "completed".equalsIgnoreCase(
                        result.getData().getAttributes().getStatus()));
    }
}
