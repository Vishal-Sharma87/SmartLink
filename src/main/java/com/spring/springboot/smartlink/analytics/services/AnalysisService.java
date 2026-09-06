package com.spring.springboot.smartlink.analytics.services;

import com.spring.springboot.smartlink.redirection.dtos.TrackPayloadDto;
import com.spring.springboot.smartlink.analytics.entities.LinkInformation;
import com.spring.springboot.smartlink.enums.Browser;
import com.spring.springboot.smartlink.enums.Device;
import com.spring.springboot.smartlink.enums.OperatingSystem;
import com.spring.springboot.smartlink.kafka.configs.KafkaTopics;
import com.spring.springboot.smartlink.kafka.payload.LinkAnalysisPayload;
import com.spring.springboot.smartlink.kafka.services.EventPublisher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AnalysisService {
    private final EventPublisher eventPublisher;
    private final KafkaTopics kafkaTopics;

    public AnalysisService(
            EventPublisher eventPublisher,
            KafkaTopics kafkaTopics) {

        this.eventPublisher = eventPublisher;
        this.kafkaTopics = kafkaTopics;

    }

    public void publish(TrackPayloadDto dto, HttpServletRequest request) {

        String ip = getClientsIp(request);

        LinkAnalysisPayload linkAnalysisPayload = LinkAnalysisPayload.builder()
                .entityIp(ip)
                .trackPayloadDto(dto)
                .build();

        eventPublisher.publish(kafkaTopics.linkAnalysis(), linkAnalysisPayload);
        log.info("Queued link analysis for short code {}", dto.getShortHash());
    }

    private String getClientsIp(HttpServletRequest request) {

        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "CF-Connecting-IP",
                "Forwarded"
        };

        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                return value.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    public LinkInformation.ClickerDeviceInfo calculateAndGetDeviceInfo(TrackPayloadDto trackPayloadDto) {
        Integer width = trackPayloadDto.getViewportWidth() == null
                ? trackPayloadDto.getScreenWidth()
                : trackPayloadDto.getViewportWidth();

        width = width == null ? 0 : width;
        Browser browser = categoriseBrowserBasedOnUserAgent(trackPayloadDto.getUserAgent());
        OperatingSystem os = categoriseOperatingSystemBasedOnUserAgent(trackPayloadDto.getUserAgent());
        Device device = categoriseDeviceBasedOnWidthAndOperatingSystem(width, os);

        return LinkInformation.ClickerDeviceInfo.builder()
                .browser(browser)
                .device(device)
                .operatingSystem(os)
                .build();
    }

    private OperatingSystem categoriseOperatingSystemBasedOnUserAgent(String userAgent) {
        if (userAgent.contains("Windows"))
            return OperatingSystem.WINDOWS;
        if (userAgent.contains("Mac"))
            return OperatingSystem.MAC;
        if (userAgent.contains("Android"))
            return OperatingSystem.ANDROID;
        if (userAgent.contains("iPhone"))
            return OperatingSystem.IOS;
        if (userAgent.contains("Linux"))
            return OperatingSystem.LINUX;
        else
            return OperatingSystem.OTHERS;

    }

    private Browser categoriseBrowserBasedOnUserAgent(String userAgent) {
        if (userAgent.contains("Edg"))
            return Browser.MICROSOFT_EDGE;
        if (userAgent.contains("Firefox"))
            return Browser.FIREFOX;
        if (userAgent.contains("Chrome"))
            return Browser.CHROME;
        if (userAgent.contains("Safari"))
            return Browser.SAFARI;
        else
            return Browser.OTHERS;

    }

    private Device categoriseDeviceBasedOnWidthAndOperatingSystem(int width, OperatingSystem os) {

        if (os == null || width <= 0) {
            return Device.OTHERS;
        }

        switch (os) {
            case ANDROID, IOS:
                if (width <= 600) {
                    return Device.MOBILE;
                }
                if (width <= 1024) {
                    return Device.TABS;
                }
                return Device.OTHERS;

            case WINDOWS, MAC, LINUX:
                if (width <= 500) {
                    // Resized desktop window
                    return Device.DESKTOPS;
                }
                if (width <= 1366) {
                    return Device.LAPTOPS;
                }
                return Device.DESKTOPS;

            default:
                return Device.OTHERS;
        }
    }
}
