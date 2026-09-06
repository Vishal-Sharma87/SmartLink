package com.spring.springboot.smartlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartLinkApplication.class, args);
    }

}
