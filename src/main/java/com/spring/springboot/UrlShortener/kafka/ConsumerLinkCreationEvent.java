package com.spring.springboot.UrlShortener.kafka;

import com.spring.springboot.UrlShortener.entity.Links;
import com.spring.springboot.UrlShortener.entity.UrlUser;
import com.spring.springboot.UrlShortener.model.LinkCreationDto;
import com.spring.springboot.UrlShortener.services.LinkService;
import com.spring.springboot.UrlShortener.services.UserService;
import com.spring.springboot.UrlShortener.services.VirusTotalService;
import com.spring.springboot.UrlShortener.services.EmailService;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class ConsumerLinkCreationEvent {

    private final UserService userService;

    private final VirusTotalService vtService;

    private final LinkService linkService;

    private final EmailService emailService;

    @KafkaListener(
            topics = "URL_SHORTENER_link_creation",
            groupId = "link-creation-group",
            containerFactory = "linkCreationKafkaListenerContainerFactory"
    )
    public void createNewLink(LinkCreationDto linkCreationDto) {
        // Step 0: Fetch user from DB
        UrlUser userInDb = userService.getUserByUserName(linkCreationDto.getOwnerUserName());


        // Step 1: Scan URL asynchronously (non-blocking)
        vtService.scanUrl(linkCreationDto.getLongUrl(), linkCreationDto.getGeneratedHash())
                .subscribe(verdict -> {
                    // This block runs when scan completes
                    // Step 2: Proceed with URL creation or mark malicious
                    // Create short URL in DB
                    Links createdLink = Links.builder()
                            .id(linkCreationDto.getId())
                            .actualUrl(linkCreationDto.getLongUrl())
                            .hashedKey(linkCreationDto.getGeneratedHash())
                            .linkCreationTime(new Date())
                            .status(verdict)
                            .ownerUserName(linkCreationDto.getOwnerUserName())
                            .abuseReports(new ArrayList<>())
                            .firstReportedTime(null)
                            .lastReportedTime(null)
                            .reportCount(0)
                            .clickCount(0)
                            .build();
                    linkService.save(createdLink);

                    emailService.sendEmail(verdict, userInDb, linkCreationDto);

                });
    }


}
