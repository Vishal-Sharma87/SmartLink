package com.spring.springboot.smartlink.kafka;

import com.spring.springboot.smartlink.email.contentbuilder.EmailContentBuilder;
import com.spring.springboot.smartlink.email.dto.EmailBody;
import com.spring.springboot.smartlink.email.services.EmailService;
import com.spring.springboot.smartlink.entity.Link;
import com.spring.springboot.smartlink.entity.User;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.model.LinkCreationDto;
import com.spring.springboot.smartlink.services.*;

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


    private final EmailContentBuilder emailContentBuilder;
    private final EmailService emailService;

    @KafkaListener(
        topics = "smart_link_link_creation", 
        groupId = "link-creation-group", 
        containerFactory = "linkCreationKafkaListenerContainerFactory"
    )
    public void createNewLink(LinkCreationDto linkCreationDto) {

        String shortUrl = linkCreationDto.getShortUrl();

        String[] parts = shortUrl.split("/");
        String generatedHash = parts[parts.length -1];

        Long id = Base62.decode(generatedHash);

        String longUrl = linkCreationDto.getLongUrl();
        String ownerUserName = linkCreationDto.getOwnerUserName();

        log.debug("Processing asynchronous link creation for short code {}", generatedHash);


        // Step 0: Fetch user from DB
        User userInDb = userService.getUserByUserName(ownerUserName);
        if (userInDb == null) {
            log.warn("Link creation rejected for hash:{}, cause: user with userName:{} not found",
                    generatedHash, ownerUserName);

            return;
        }

        Verdict verdict = vtService.scanUrl(longUrl, generatedHash).block();
        if (verdict == null){
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
                .firstReportedTime(null)
                .lastReportedTime(null)
                .reportCount(0)
                .clickCount(0)
                .build();

        linkService.save(createdLink);
        log.info("Asynchronous link creation completed for short code {} with verdict {}",
                generatedHash, verdict);

        EmailBody linkCreated;
        if (Verdict.MALICIOUS.equals(verdict)){
            linkService.incrementAndGetMaliciousCount(ownerUserName);
            linkCreated = emailContentBuilder.createdLinkIsMaliciousContent(
                    ownerUserName,
                    userInDb.getEmail(),
                    longUrl,
                    shortUrl
            );

        }else{
            linkCreated = emailContentBuilder.linkCreatedContent(
                    ownerUserName,
                    userInDb.getEmail(),
                    verdict,
                    longUrl,
                    shortUrl
            );
        }

        emailService.sendEmail(linkCreated);
    }

}
