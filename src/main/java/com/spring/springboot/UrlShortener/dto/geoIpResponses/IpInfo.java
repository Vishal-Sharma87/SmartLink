package com.spring.springboot.UrlShortener.dto.geoIpResponses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IpInfo {

    @JsonProperty("ip")
    private String ip;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("country")
    private String country;

    @JsonProperty("continent_code")
    private String continentCode;

    @JsonProperty("continent")
    private String continent;
}
