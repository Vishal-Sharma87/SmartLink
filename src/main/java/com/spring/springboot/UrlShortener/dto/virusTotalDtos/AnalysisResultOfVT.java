package com.spring.springboot.UrlShortener.dto.virusTotalDtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AnalysisResultOfVT {
    @JsonProperty("data")
    private DataObject data;

    @Data
    public static class DataObject {
        @JsonProperty("attributes")
        private Attributes attributes;
    }

    @Data
    public static class Attributes {
        @JsonProperty("stats")
        private Stats stats;

        @JsonProperty("date")
        private long date;      // epoch seconds

        @JsonProperty("status")
        private String status;  // e.g. "completed"
    }

    @Data
    public static class Stats {
        @JsonProperty("malicious")
        private int malicious;

        @JsonProperty("suspicious")
        private int suspicious;

        @JsonProperty("undetected")
        private int undetected;

        @JsonProperty("harmless")
        private int harmless;

        @JsonProperty("timeout")
        private int timeout;

        public int getTotal() {
            return malicious + suspicious + undetected + harmless + timeout;
        }
    }
}
