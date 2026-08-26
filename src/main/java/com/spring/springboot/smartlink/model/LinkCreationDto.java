package com.spring.springboot.smartlink.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkCreationDto {
    private String longUrl;
    private String ownerUserName;
    private String generatedHash;
    private Long id;
}
