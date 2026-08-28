package com.spring.springboot.smartlink.kafka;

import com.spring.springboot.smartlink.model.LinkAnalysisDto;
import com.spring.springboot.smartlink.services.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class ConsumerExistedLinkAnalysisEvent {


    private final AnalysisService analysisService;

    // For LinkAnalysis topic
    @KafkaListener(
            topics = "smart_link_existed_link_analysis",
            groupId = "link-analysis-group",
            containerFactory = "linkAnalysisKafkaListenerContainerFactory"
    )
    public void analysis(LinkAnalysisDto linkAnalysisDto) {

        analysisService.analyze(linkAnalysisDto);
        log.debug("Processed link analysis event for short code {}", linkAnalysisDto.getTrackPayloadDto().getShortHash());

    }
}
