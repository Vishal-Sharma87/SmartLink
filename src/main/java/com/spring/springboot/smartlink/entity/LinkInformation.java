package com.spring.springboot.smartlink.entity;

import com.spring.springboot.smartlink.dto.geoIpResponses.IpInfo;
import com.spring.springboot.smartlink.enums.Browser;
import com.spring.springboot.smartlink.enums.Device;
import com.spring.springboot.smartlink.enums.OperatingSystem;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document(collection = "link_analytics")
public class LinkInformation {

    @Id
    private ObjectId linkInformationId;

    @Indexed
    private String associatedShortHash;

    private IpInfo entityIpInformation;

    private ClickerDeviceInfo deviceInfo;

    private Instant timeOfClick;

    private String timeZone;

    @Data
    @Builder
    public static class ClickerDeviceInfo {
        private Browser browser;

        private Device device;

        private OperatingSystem operatingSystem;
    }

}
