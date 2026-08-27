package com.spring.springboot.smartlink.services;

import com.spring.springboot.smartlink.dto.TrackPayloadDto;
import com.spring.springboot.smartlink.dto.geoIpResponses.IpInfo;
import com.spring.springboot.smartlink.entity.Link;
import com.spring.springboot.smartlink.entity.LinkInformation;
import com.spring.springboot.smartlink.enums.Browser;
import com.spring.springboot.smartlink.enums.Device;
import com.spring.springboot.smartlink.enums.OperatingSystem;
import com.spring.springboot.smartlink.model.LinkAnalysisDto;
import io.ipinfo.api.IPinfoLite;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponseLite;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class AnalysisService {

    private final KafkaTemplate<String, LinkAnalysisDto> kafkaTemplate;
    private final LinkService linkService;
    private final LinkInformationService linkInformationService;

    private final IPinfoLite client;

    public AnalysisService(
            @Value("${ipinfo.token}") String ipInfoToken,
            KafkaTemplate<String, LinkAnalysisDto> kafkaTemplate,
            LinkService linkService,
            LinkInformationService linkInformationService) {

        this.kafkaTemplate = kafkaTemplate;
        this.linkService = linkService;
        this.linkInformationService = linkInformationService;
        this.client = new IPinfoLite.Builder()
                .setToken(ipInfoToken)
                .build();

    }

    public void publish(TrackPayloadDto dto, HttpServletRequest request) {

        String ip = getClientsIp(request);

        LinkAnalysisDto linkAnalysisDto = LinkAnalysisDto.builder()
                .entityIp(ip)
                .trackPayloadDto(dto)
                .build();
        kafkaTemplate.send("smart_link_existed_link_analysis", linkAnalysisDto);
        log.info("Queued link analysis for short code {}", dto.getShortHash());
    }

    public void analyze(LinkAnalysisDto linkAnalysisDto) {

        String hash = linkAnalysisDto.getTrackPayloadDto().getShortHash();
        Link linkInDb = linkService.getLinkByHash(hash);

        if (hash == null || hash.isEmpty() || linkInDb == null) {
            log.warn("Link analytics event dropped because short code {} could not be resolved", hash);
            return;
        }

        String ip = linkAnalysisDto.getEntityIp();
        IpInfo ipInfo = getIpRelatedGeoInfoResponse(ip);

        int width;
        if (linkAnalysisDto.getTrackPayloadDto().getViewportWidth() == null
                && linkAnalysisDto.getTrackPayloadDto().getScreenWidth() == null) {
            width = 0;
        } else {
            width = linkAnalysisDto.getTrackPayloadDto().getViewportWidth() == null
                    ? linkAnalysisDto.getTrackPayloadDto().getScreenWidth()
                    : linkAnalysisDto.getTrackPayloadDto().getViewportWidth();
        }

        Browser browser = categoriseBrowserBasedOnUserAgent(linkAnalysisDto.getTrackPayloadDto().getUserAgent());

        OperatingSystem os = categoriseOperatingSystemBasedOnUserAgent(
                linkAnalysisDto.getTrackPayloadDto().getUserAgent());

        Device device = categoriseDeviceBasedOnWidthAndOperatingSystem(width, os);

        linkInDb.incrementClickCount();
        linkService.save(linkInDb);

        LinkInformation.ClickerDeviceInfo deviceInfo = LinkInformation.ClickerDeviceInfo.builder()
                .browser(browser)
                .device(device)
                .operatingSystem(os)
                .build();

        LinkInformation linkInformation = LinkInformation.builder()
                .associatedShortHash(hash)
                .entityIpInformation(ipInfo)
                .timeZone(linkAnalysisDto.getTrackPayloadDto().getTimezone())
                .timeOfClick(Instant.now())
                .deviceInfo(deviceInfo)
                .build();

        linkInformationService.save(linkInformation);
        log.info("Link analysis completed for short code {} with browser {}, operating system {}, and device {}", hash, browser, os, device);

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

    private IpInfo getIpRelatedGeoInfoResponse(String ip) {
        try {
            IPResponseLite details = client.lookupIP(ip);

            IpInfo response = new IpInfo();

            response.setIp(details.getIp());
            response.setCountryCode(details.getCountryCode());
            response.setCountry(details.getCountry());
            response.setContinentCode(details.getContinentCode());
            response.setContinent(details.getContinent());

            return response;

        } catch (RateLimitedException e) {
            log.error("Rate Limited For IpInfoLite lookup, ip {}", ip);
            log.error("IP information lookup failed due to rate limiting", e);
            return null;
        }
    }
}
