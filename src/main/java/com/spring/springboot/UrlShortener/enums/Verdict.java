package com.spring.springboot.UrlShortener.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.ToString;

@ToString
public enum Verdict {
    SAFE, SUSPICIOUS, UNVERIFIED, MALICIOUS, PENDING_REVERIFICATION;

    @JsonValue
    public String toJson() {
        return this.name();
    }
}