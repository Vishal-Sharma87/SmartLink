package com.spring.springboot.smartlink.kafka;

import com.spring.springboot.smartlink.entity.Link;
import com.spring.springboot.smartlink.entity.User;
import com.spring.springboot.smartlink.model.LinkCreationDto;
import com.spring.springboot.smartlink.services.LinkService;
import com.spring.springboot.smartlink.services.UserService;
import com.spring.springboot.smartlink.services.VirusTotalService;
import com.spring.springboot.smartlink.services.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumerLinkCreationEvent {

    private final UserService userService;

    private final VirusTotalService vtService;

    private final LinkService linkService;

    private final EmailService emailService;

    @KafkaListener(
        topics = "smart_link_link_creation", 
        groupId = "link-creation-group", 
        containerFactory = "linkCreationKafkaListenerContainerFactory"
    )
    public void createNewLink(LinkCreationDto linkCreationDto) {
        log.debug("Processing asynchronous link creation for short code {}", linkCreationDto.getGeneratedHash());
        // Step 0: Fetch user from DB
        User userInDb = userService.getUserByUserName(linkCreationDto.getOwnerUserName());
        if (userInDb == null) {
            log.warn("Owner user {} was not found for short code {} during asynchronous link creation",
                    linkCreationDto.getOwnerUserName(), linkCreationDto.getGeneratedHash());
        }

        // Step 1: Scan URL asynchronously (non-blocking)
        vtService.scanUrl(linkCreationDto.getLongUrl(), linkCreationDto.getGeneratedHash())
                .subscribe(verdict -> {
                    // This block runs when scan completes
                    // Step 2: Proceed with URL creation or mark malicious
                    // Create short URL in DB
                    Link createdLink = Link.builder()
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
                    log.info("Asynchronous link creation completed for short code {} with verdict {}",
                            linkCreationDto.getGeneratedHash(), verdict);

                    emailService.sendEmail(verdict, userInDb, linkCreationDto);

                });
    }

}
