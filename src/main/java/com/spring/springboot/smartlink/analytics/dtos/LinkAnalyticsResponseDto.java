package com.spring.springboot.smartlink.analytics.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkAnalyticsResponseDto {
    private String shortHash;
    private String actualUrl;
    private Integer totalClicks;
    private Integer uniqueCountries;
    private SummaryItem topCountry;
    private List<SummaryItem> countries;
    private List<SummaryItem> continents;
    private List<SummaryItem> devices;
    private List<SummaryItem> browsers;
    private List<SummaryItem> operatingSystems;
    private List<ClickItem> recentClicks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryItem {
        private String label;
        private long count;
        private double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClickItem {
        private Instant time;
        private String country;
        private String continent;
        private String device;
        private String browser;
        private String operatingSystem;
        private String timeZone;
    }
}
