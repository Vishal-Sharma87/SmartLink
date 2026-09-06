package com.spring.springboot.smartlink.analytics.services;

import com.spring.springboot.smartlink.analytics.dtos.LinkAnalyticsResponseDto;
import com.spring.springboot.smartlink.analytics.entities.LinkInformation;
import com.spring.springboot.smartlink.link.dtos.LinkAsResponseDto;
import com.spring.springboot.smartlink.link.services.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkAnalyticsService {

    private final LinkService linkService;
    private final LinkInformationService linkInformationService;

    public LinkAnalyticsResponseDto summarize(String userName, String shortHash) {
        LinkAsResponseDto link = linkService.findLinkOfUser(shortHash, userName);

        String actualUrl = link.getActualUrl();
        int fallbackClickCount = link.getClickCnt() == null ? 0 : link.getClickCnt();

        List<LinkInformation> clicks = linkInformationService.findByShortHash(shortHash);
        int totalClicks = Math.max(fallbackClickCount, clicks.size());
        return LinkAnalyticsResponseDto.builder()
                .shortHash(shortHash)
                .actualUrl(actualUrl)
                .totalClicks(totalClicks)
                .uniqueCountries((int) clicks.stream().map(this::country).filter(this::present).distinct().count())
                .topCountry(summary(countryCounts(clicks), totalClicks).stream().findFirst().orElse(null))
                .countries(summary(countryCounts(clicks), totalClicks))
                .continents(summary(count(clicks, this::continent), totalClicks))
                .devices(summary(count(clicks, this::device), totalClicks))
                .browsers(summary(count(clicks, this::browser), totalClicks))
                .operatingSystems(summary(count(clicks, this::operatingSystem), totalClicks))
                .recentClicks(clicks.stream().limit(20).map(this::click).toList())
                .build();
    }

    private Map<String, Long> countryCounts(List<LinkInformation> clicks) {
        return count(clicks, this::country);
    }

    private Map<String, Long> count(List<LinkInformation> clicks, Function<LinkInformation, String> extractor) {
        return clicks.stream().map(extractor).filter(this::present)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
    }

    private List<LinkAnalyticsResponseDto.SummaryItem> summary(Map<String, Long> counts, int totalClicks) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(entry -> LinkAnalyticsResponseDto.SummaryItem.builder()
                        .label(entry.getKey()).count(entry.getValue())
                        .percentage(totalClicks == 0 ? 0 : entry.getValue() * 100.0 / totalClicks).build())
                .toList();
    }

    private LinkAnalyticsResponseDto.ClickItem click(LinkInformation info) {
        return LinkAnalyticsResponseDto.ClickItem.builder()
                .time(info.getTimeOfClick()).country(country(info)).continent(continent(info))
                .device(device(info)).browser(browser(info)).operatingSystem(operatingSystem(info))
                .timeZone(info.getTimeZone()).build();
    }

    private String country(LinkInformation info) {
        return info.getEntityIpInformation() == null ? null
                : first(info.getEntityIpInformation().getCountry(), info.getEntityIpInformation().getCountryCode());
    }

    private String continent(LinkInformation info) {
        return info.getEntityIpInformation() == null ? null
                : first(info.getEntityIpInformation().getContinent(), info.getEntityIpInformation().getContinentCode());
    }

    private String device(LinkInformation info) {
        return info.getDeviceInfo() == null || info.getDeviceInfo().getDevice() == null ? null
                : prettify(info.getDeviceInfo().getDevice().name());
    }

    private String browser(LinkInformation info) {
        return info.getDeviceInfo() == null || info.getDeviceInfo().getBrowser() == null ? null
                : prettify(info.getDeviceInfo().getBrowser().name());
    }

    private String operatingSystem(LinkInformation info) {
        return info.getDeviceInfo() == null || info.getDeviceInfo().getOperatingSystem() == null ? null
                : prettify(info.getDeviceInfo().getOperatingSystem().name());
    }

    private String first(String primary, String fallback) {
        return present(primary) ? primary : fallback;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private String prettify(String value) {
        String[] words = value.replace('_', ' ').toLowerCase().split(" ");
        return java.util.Arrays.stream(words)
                .filter(word -> !word.isBlank())
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
