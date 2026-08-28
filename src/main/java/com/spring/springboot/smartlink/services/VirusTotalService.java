package com.spring.springboot.smartlink.services;

import com.spring.springboot.smartlink.dto.virusTotalDtos.AnalysisIdOfVT;
import com.spring.springboot.smartlink.dto.virusTotalDtos.AnalysisResultOfVT;
import com.spring.springboot.smartlink.enums.Verdict;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class VirusTotalService {

    private final WebClient webClient;

    @Value("${virustotal.api-key}")
    private String VTApiKey;

    @Value("${virustotal.analysis-url}")
    private String apiToGetAnalysisId;

    private final VerdictEvaluationService finalVerdict;

    /**
     * Scans a URL and returns a FinalVerdict asynchronously.
     */
    public Mono<Verdict> scanUrl(String url, String hash) {
        log.debug("Starting URL safety scan for short code {}", hash);

        return webClient.post()
                .uri(apiToGetAnalysisId)
                .header("x-apikey", VTApiKey)
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
                                return finalVerdict.evaluate(stats, hash, url);
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
                .header("x-apikey", VTApiKey)
                .retrieve()
                .bodyToMono(AnalysisResultOfVT.class)
                // Only emit the result when scan is fully completed
                .filter(result -> "completed".equalsIgnoreCase(
                        result.getData().getAttributes().getStatus()));
    }
}
