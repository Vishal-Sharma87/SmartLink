package com.spring.springboot.smartlink.link.services;

import com.spring.springboot.smartlink.advices.exceptions.LinkNotFoundExceptionSmartLink;
import com.spring.springboot.smartlink.analytics.services.LinkScanResponseService;
import com.spring.springboot.smartlink.apiresponse.ResponseMessage;
import com.spring.springboot.smartlink.configurations.ApplicationConfigs;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.link.dtos.LinkAsResponseDto;
import com.spring.springboot.smartlink.link.dtos.LinkCreationResponseDto;
import com.spring.springboot.smartlink.link.dtos.LinkQueryResponseDto;
import com.spring.springboot.smartlink.link.entities.Link;
import com.spring.springboot.smartlink.analytics.entities.LinkScanResponse;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.kafka.configs.KafkaTopics;
import com.spring.springboot.smartlink.kafka.payload.LinkCreationPayload;
import com.spring.springboot.smartlink.kafka.services.EventPublisher;
import com.spring.springboot.smartlink.link.enums.LinkStatus;
import com.spring.springboot.smartlink.redis.services.RedisService;
import com.spring.springboot.smartlink.link.repositories.LinkRepository;

import com.spring.springboot.smartlink.utils.Base62;
import com.spring.springboot.smartlink.virustotal.services.VirusTotalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkService {

    private final LinkRepository linkRepository;
    private final RedisService redisService;
    private final MongoLinkService mongoLinkService;
    private final LinkScanResponseService linkScanResponseService;
    private final ExceptionMessages exceptionMessages;
    private final EventPublisher eventPublisher;
    private final KafkaTopics kafkaTopics;

    // just to test synchronous link creation
    private final VirusTotalService virusTotalService;
    private final ApplicationConfigs applicationConfigs;
    private final ResponseMessage responseMessage;

    public LinkCreationResponseDto initializeCreation(String urlToShort, String userEmail) {
        Long urlCounter = redisService.getUrlCounter();

        String shortCode = Base62.encode(urlCounter);
        String shortUrl = buildShortUrl(shortCode);

        LinkCreationPayload dtoToCreate = new LinkCreationPayload(
                urlToShort,
                userEmail,
                shortUrl
        );

        eventPublisher.publish(kafkaTopics.linkCreation(), dtoToCreate);

        log.info("Asynchronous link creation queued for shortCode={}", shortCode);

        return new LinkCreationResponseDto(
                responseMessage.linkCreationPending(),
                LinkStatus.PENDING,
                shortUrl
        );
    }

    public LinkQueryResponseDto getAllLinksOfAnUser(String userEmail) {
        List<Link> allLinksOfAnUser = mongoLinkService.getAllLinksOfAnUser(userEmail);
        List<LinkAsResponseDto> allLinksOfAnUserDtos = allLinksOfAnUser
                .stream()
                .map(this::convertActualLinkToResponseLink)
                .toList();

        return new LinkQueryResponseDto(
                allLinksOfAnUserDtos,
                responseMessage.allLinksOfUserMessage()
        );
    }

    public String deleteLinkOfUser(String userEmail, String shortCode) {

        if (shortCode.isEmpty() || isNotInCounterRange(Base62.decode(shortCode, exceptionMessages.invalidLinkHash())))
            throw new LinkNotFoundExceptionSmartLink(String.format(exceptionMessages.linkNotFound(), shortCode));

        log.info("Link deletion initiated for shortCode={}", shortCode);
        mongoLinkService.deleteLinkOfUserByShortCode(shortCode, userEmail);
        log.info("Link deletion completed for shortCode={}", shortCode);

        return responseMessage.linkDeleted();
    }

    public String deleAllLinkOfUser(String userEmail) {
        mongoLinkService.deleteAllLinksOfAnUser(userEmail);

        return responseMessage.allLinksOfUserDeleted();
    }

    public LinkCreationResponseDto initializeCreationSync(String urlToShort, String email) {

        /*
         * will do following steps
         * 1 -> get counter value from redis
         * 2 -> convert counter to hash
         * 3 -> scan the url through VT SERVICE's API synchronously
         * 4 -> save the link into DB
         * 5 -> return short hash
         */

        Long urlCounter = redisService.getUrlCounter();

        String shortCode = Base62.encode(urlCounter);

        Verdict verdict = virusTotalService.scanUrl(urlToShort, shortCode).block();

        save(Link.builder()
                .originalUrl(urlToShort)
                .shortCode(shortCode)
                .createdAt(Instant.now())
                .status(verdict)
                .ownerEmail(email)
                .abuseReports(new ArrayList<>())
                .build());

        log.info("Synchronous link creation completed for shortCode={} with verdict={}", shortCode, verdict);

        String shortUrl = buildShortUrl(shortCode);

        return new LinkCreationResponseDto(
                responseMessage.linkCreated(),
                LinkStatus.ACTIVE,
                shortUrl
        );
    }

    public void save(Link link) {
        linkRepository.save(link);
    }

    public LinkAsResponseDto findLinkOfUser(String shortCode, String userEmail) {

        if (shortCode.isEmpty() || isNotInCounterRange(Base62.decode(shortCode, exceptionMessages.invalidLinkHash())))
            throw new LinkNotFoundExceptionSmartLink(String.format(exceptionMessages.linkNotFound(), shortCode));

        Link link = mongoLinkService.getLinkOfAnUserByShortCode(shortCode, userEmail);
        return convertActualLinkToResponseLink(link);
    }

    public Link getLinkByShortCode(String shortCode) {
        return linkRepository.findById(shortCode).orElse(null);
    }

    public LinkScanResponse getLinkScanDetails(String shortCode) {
        return linkScanResponseService.getLinkScanResponse(shortCode);
    }

    public boolean isNotInCounterRange(Long counterToCheck) {
        return redisService.isNotInRange(counterToCheck);
    }

    private String buildShortUrl(String shortCode) {
        return applicationConfigs.shortUrlPrefix() + "/" + shortCode;
    }

    private LinkAsResponseDto convertActualLinkToResponseLink(Link link) {
        return new LinkAsResponseDto(
                link.getOriginalUrl(),
                link.getShortCode(),
                link.getStatus(),
                link.getClickCount(),
                link.getReportCount(),
                link.getCreatedAt()
        );
    }

    public boolean incrementClickCountIfExists(String shortCode) {
        return mongoLinkService.incrementClickCountIfExists(shortCode);
    }
}
