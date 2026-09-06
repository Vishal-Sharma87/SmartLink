package com.spring.springboot.smartlink.kafka.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkCreationPayload {
    private String longUrl;
    private String ownerUserName;
    private String shortUrl;
}
