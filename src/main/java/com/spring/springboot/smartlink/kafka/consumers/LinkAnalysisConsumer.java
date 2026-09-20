package com.spring.springboot.smartlink.kafka.consumers;

import com.spring.springboot.smartlink.analytics.services.AnalysisService;
import com.spring.springboot.smartlink.analytics.services.LinkInformationService;
import com.spring.springboot.smartlink.geoip.model.IpInfo;
import com.spring.springboot.smartlink.geoip.services.IpInfoService;
import com.spring.springboot.smartlink.kafka.payload.LinkAnalysisPayload;
import com.spring.springboot.smartlink.link.services.LinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import com.spring.springboot.smartlink.analytics.entities.LinkInformation;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class LinkAnalysisConsumer {

    private final LinkService linkService;
    private final LinkInformationService linkInformationService;
    private final IpInfoService ipInfoService;
    private final AnalysisService analysisService;

    public LinkAnalysisConsumer(
            LinkService linkService,
            LinkInformationService linkInformationService,
            IpInfoService ipInfoService,
            AnalysisService analysisService) {

        this.linkService = linkService;
        this.linkInformationService = linkInformationService;
        this.ipInfoService = ipInfoService;
        this.analysisService = analysisService;
    }

    @KafkaListener(topics = "${smart-link.kafka.topic.link-analysis}", groupId = "${smart-link.kafka.consumer.link-analysis-group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void analysis(LinkAnalysisPayload payload) {
        String shortCode = payload.trackPayloadDto().shortCode();

        if (!linkService.incrementClickCountIfExists(shortCode)) {
            log.warn("Link analytics event dropped because short code {} could not be resolved", shortCode);
            return;
        }

        LinkInformation.ClickerDeviceInfo deviceInfo = analysisService
                .calculateAndGetDeviceInfo(payload.trackPayloadDto());

        String ip = payload.entityIp();
        IpInfo ipInfo = ipInfoService.fetchIpInfo(ip);

        LinkInformation linkInformation = LinkInformation.builder()
                .shortCode(shortCode)
                .entityIpInformation(ipInfo)
                .timeZone(payload.trackPayloadDto().timezone())
                .timeOfClick(Instant.now())
                .deviceInfo(deviceInfo)
                .build();

        linkInformationService.save(linkInformation);
        log.debug("Link analytics event persisted for shortCode={}", shortCode);
    }

}
