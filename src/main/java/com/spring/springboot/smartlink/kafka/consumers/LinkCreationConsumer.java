package com.spring.springboot.smartlink.kafka.consumers;

import com.spring.springboot.smartlink.email.services.EmailService;
import com.spring.springboot.smartlink.link.entities.Link;
import com.spring.springboot.smartlink.user.entities.User;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.kafka.payload.LinkCreationPayload;
import com.spring.springboot.smartlink.link.services.LinkService;
import com.spring.springboot.smartlink.utils.*;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;

import com.spring.springboot.smartlink.user.services.UserService;
import com.spring.springboot.smartlink.virustotal.services.VirusTotalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;

@Service
@Slf4j
public class LinkCreationConsumer {

    private final UserService userService;
    private final VirusTotalService vtService;
    private final LinkService linkService;
    private final EmailService emailService;
    private final ExceptionMessages exceptionMessages;

    public LinkCreationConsumer(
            UserService userService,
            VirusTotalService vtService,
            LinkService linkService,
            EmailService emailService,
            ExceptionMessages exceptionMessages) {

        this.userService = userService;
        this.vtService = vtService;
        this.linkService = linkService;
        this.emailService = emailService;
        this.exceptionMessages = exceptionMessages;
    }

    @KafkaListener(topics = "${smart-link.kafka.topic.link-creation}", groupId = "${smart-link.kafka.consumer.link-creation-group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void createNewLink(LinkCreationPayload linkCreationPayload) {

        String shortUrl = linkCreationPayload.getShortUrl();

        String[] parts = shortUrl.split("/");
        String generatedHash = parts[parts.length - 1];

        Long id = Base62.decode(generatedHash, exceptionMessages.invalidLinkHash());

        String longUrl = linkCreationPayload.getLongUrl();
        String ownerUserName = linkCreationPayload.getOwnerUserName();

        log.debug("Processing asynchronous link creation for short code {}", generatedHash);

        // Step 0: Fetch user from DB
        User userInDb = userService.getUserByUserName(ownerUserName);
        if (userInDb == null) {
            log.warn("Link creation rejected for hash:{}, cause: user with userName:{} not found",
                    generatedHash, ownerUserName);

            return;
        }

        Verdict verdict = vtService.scanUrl(longUrl, generatedHash).block();
        if (verdict == null) {
            log.warn("Scanned Verdicts is NULL for shortUrl:{}", shortUrl);
            return;
        }

        Link createdLink = Link.builder()
                .id(id)
                .actualUrl(longUrl)
                .hashedKey(generatedHash)
                .linkCreationTime(new Date())
                .status(verdict)
                .ownerUserName(ownerUserName)
                .abuseReports(new ArrayList<>())
                .reportCount(0)
                .clickCount(0)
                .build();

        linkService.save(createdLink);
        log.info("Asynchronous link creation completed for short code {} with verdict {}",
                generatedHash, verdict);

        if (Verdict.MALICIOUS.equals(verdict)) {
            userService.incrementAndGetMaliciousCount(ownerUserName);
            emailService.sendLinkCreatedFoundMaliciousEmail(ownerUserName, userInDb.getEmail(), shortUrl, longUrl);
        } else {
            emailService.sendLinkCreatedEmail(ownerUserName, userInDb.getEmail(), shortUrl, longUrl, verdict);
        }

    }

}
