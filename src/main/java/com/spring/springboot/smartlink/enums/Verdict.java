package com.spring.springboot.smartlink.enums;

import com.fasterxml.jackson.annotation.JsonValue;


public enum Verdict {
    SAFE, SUSPICIOUS, UNVERIFIED, MALICIOUS, PENDING_REVERIFICATION;

    @JsonValue
    public String toJson() {
        return this.name();
    }
}