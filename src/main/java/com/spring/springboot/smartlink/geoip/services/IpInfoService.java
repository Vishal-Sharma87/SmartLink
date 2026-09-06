package com.spring.springboot.smartlink.geoip.services;

import com.spring.springboot.smartlink.geoip.configs.IpInfoConfigs;
import com.spring.springboot.smartlink.geoip.model.IpInfo;
import io.ipinfo.api.IPinfoLite;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponseLite;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IpInfoService {

    private final IPinfoLite client;

    public IpInfoService(IpInfoConfigs configs) {
        this.client = new IPinfoLite.Builder()
                .setToken(configs.token())
                .build();
    }

    public IpInfo fetchIpInfo(String ip) {
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
