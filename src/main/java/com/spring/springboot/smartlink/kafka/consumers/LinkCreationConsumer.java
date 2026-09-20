package com.spring.springboot.smartlink.kafka.consumers;

import com.spring.springboot.smartlink.email.services.EmailService;
import com.spring.springboot.smartlink.link.entities.Link;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.kafka.payload.LinkCreationPayload;
import com.spring.springboot.smartlink.link.services.LinkService;

import com.spring.springboot.smartlink.user.services.UserService;
import com.spring.springboot.smartlink.virustotal.services.VirusTotalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;

@Service
@Slf4j
public class LinkCreationConsumer {

    private final UserService userService;
    private final VirusTotalService vtService;
    private final LinkService linkService;
    private final EmailService emailService;

    public LinkCreationConsumer(
            UserService userService,
            VirusTotalService vtService,
            LinkService linkService,
            EmailService emailService){

        this.userService = userService;
        this.vtService = vtService;
        this.linkService = linkService;
        this.emailService = emailService;
    }

    @KafkaListener(topics = "${smart-link.kafka.topic.link-creation}", groupId = "${smart-link.kafka.consumer.link-creation-group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void createNewLink(LinkCreationPayload linkCreationPayload) {

        String shortUrl = linkCreationPayload.shortUrl();

        String[] parts = shortUrl.split("/");
        String shortCode = parts[parts.length - 1];

        String originalUrl = linkCreationPayload.originalUrl();
        String ownerEmail = linkCreationPayload.ownerEmail();

        log.debug("Processing asynchronous link creation for shortCode={}", shortCode);

        // Step 0: Fetch user from DB
        if (!userService.existsByEmail(ownerEmail)) {
            log.warn("Asynchronous link creation skipped because the owner was not found for shortCode={}", shortCode);
            return;
        }

        Verdict verdict = vtService.scanUrl(originalUrl, shortCode).block();
        if (verdict == null) {
            log.warn("Asynchronous link creation skipped because safety analysis returned no verdict for shortCode={}",
                    shortCode);
            return;
        }

        Link createdLink = Link.builder()
                .originalUrl(originalUrl)
                .shortCode(shortCode)
                .createdAt(Instant.now())
                .status(verdict)
                .ownerEmail(ownerEmail)
                .abuseReports(new ArrayList<>())
                .build();

        linkService.save(createdLink);
        log.info("Asynchronous link creation completed for shortCode={} with verdict={}",
                shortCode, verdict);

        if (Verdict.MALICIOUS.equals(verdict)) {
            userService.incrementAndGetMaliciousCount(ownerEmail);
            emailService.sendLinkCreatedFoundMaliciousEmail(ownerEmail, shortUrl, originalUrl);
        } else {
            emailService.sendLinkCreatedEmail(
                    ownerEmail,
                    originalUrl,
                    shortCode,
                    verdict
            );
        }

    }

}
