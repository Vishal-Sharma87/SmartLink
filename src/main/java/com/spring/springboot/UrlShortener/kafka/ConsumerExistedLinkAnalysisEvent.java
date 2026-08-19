package com.spring.springboot.UrlShortener.kafka;

import com.spring.springboot.UrlShortener.model.LinkAnalysisDto;
import com.spring.springboot.UrlShortener.services.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class ConsumerExistedLinkAnalysisEvent {


    private final AnalysisService analysisService;

    // For LinkAnalysis topic
    @KafkaListener(
            topics = "URL_SHORTENER_existed_link_analysis",
            groupId = "link-analysis-group",
            containerFactory = "linkAnalysisKafkaListenerContainerFactory"
    )
    public void analysis(LinkAnalysisDto linkAnalysisDto) {

        analysisService.analyze(linkAnalysisDto);

    }
}
