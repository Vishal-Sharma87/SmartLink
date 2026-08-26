package com.spring.springboot.smartlink.dto.virusTotalDtos;

import lombok.Data;

@Data
public class AnalysisResultOfVT {
    private DataObject data;

    @Data
    public static class DataObject {
        private Attributes attributes;
    }

    @Data
    public static class Attributes {
        private Stats stats;

        private long date;      // epoch seconds

        private String status;  // e.g. "completed"
    }

    @Data
    public static class Stats {
        private int malicious;

        private int suspicious;

        private int undetected;

        private int harmless;

        private int timeout;
    }
}
