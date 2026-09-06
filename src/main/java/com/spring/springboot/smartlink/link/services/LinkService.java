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
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
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

    public LinkCreationResponseDto initializeCreation(String urlToShort, String userName) {
        Long urlCounter = redisService.getUrlCounter();

        String shortCode = Base62.encode(urlCounter);
        String shortUrl = buildShortUrl(shortCode);

        LinkCreationPayload dtoToCreate = LinkCreationPayload.builder()
                .shortUrl(shortUrl)
                .longUrl(urlToShort)
                .ownerUserName(userName)
                .build();

        eventPublisher.publish(kafkaTopics.linkCreation(), dtoToCreate);

        log.info("Queued asynchronous link creation for short code {} and user {}", shortCode, userName);

        return LinkCreationResponseDto
                .builder()
                .shortUrl(shortUrl)
                .message(responseMessage.linkCreationPending())
                .status(LinkStatus.PENDING)
                .build();
    }

    public LinkQueryResponseDto getAllLinksOfAnUser(String userName) {
        List<Link> allLinksOfAnUser = mongoLinkService.getAllLinksOfAnUser(userName);
        List<LinkAsResponseDto> allLinksOfAnUserDtos = allLinksOfAnUser
                .stream()
                .map(this::convertActualLinkToResponseLink)
                .toList();

        return LinkQueryResponseDto.builder()
                .links(allLinksOfAnUserDtos)
                .message(responseMessage.allLinksOfUserMessage())
                .build();
    }

    public String deleteLinkOfUser(String userName, String hash) {
        Long idToDelete = Base62.decode(hash, exceptionMessages.invalidLinkHash());

        if (hash.isEmpty() || isAfterLastCounter(idToDelete))
            throw new LinkNotFoundExceptionSmartLink(String.format(exceptionMessages.linkNotFound(), hash));

        mongoLinkService.deleteLinkOfUserById(idToDelete, userName);

        return responseMessage.linkDeleted();
    }

    public String deleAllLinkOfUser(String userName) {
        mongoLinkService.deleteAllLinksOfAnUser(userName);

        return responseMessage.allLinksOfUserDeleted();
    }

    public LinkCreationResponseDto initializeCreationSync(String urlToShort, String userName) {

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

        Mono<Verdict> verdict = virusTotalService.scanUrl(urlToShort, shortCode);

        save(Link.builder()
                .id(urlCounter)
                .actualUrl(urlToShort)
                .hashedKey(shortCode)
                .linkCreationTime(new Date())
                .status(verdict.block())
                .ownerUserName(userName)
                .abuseReports(new ArrayList<>())
                .reportCount(0)
                .clickCount(0)
                .build());

        log.info("Synchronous link creation completed for short code {} and user {} with verdict {}", shortCode,
                userName, verdict.block());

        String shortUrl = buildShortUrl(shortCode);

        return LinkCreationResponseDto
                .builder()
                .shortUrl(shortUrl)
                .message(responseMessage.linkCreated())
                .status(LinkStatus.ACTIVE)
                .build();
    }

    public void save(Link createdLink) {
        linkRepository.save(createdLink);
        log.debug("Saved link with short code {} and verdict {}", createdLink.getHashedKey(), createdLink.getStatus());
    }

    public LinkAsResponseDto findLinkOfUser(String hash, String userName) {
        Long idToFind = Base62.decode(hash, exceptionMessages.invalidLinkHash());

        if (hash.isEmpty() || isAfterLastCounter(idToFind))
            throw new LinkNotFoundExceptionSmartLink(String.format(exceptionMessages.linkNotFound(), hash));

        Link link = mongoLinkService.getLinkOfAnUserById(String.valueOf(idToFind), userName);
        return convertActualLinkToResponseLink(link);
    }

    public Link getLinkByHash(String hash) {
        Link link = linkRepository.findById(Base62.decode(hash, exceptionMessages.invalidLinkHash())).orElse(null);
        if (link == null) {
            log.debug("No link found for short code {} during analytics processing", hash);
        }
        return link;
    }

    public LinkScanResponse getLinkScanDetails(String shortCode) {
        return linkScanResponseService.getLinkScanResponse(shortCode);
    }

    public boolean isAfterLastCounter(Long counterToCheck) {
        return redisService.isAfter(counterToCheck);
    }

    private String buildShortUrl(String shortCode) {
        return applicationConfigs.shortUrlPrefix() + "/" + shortCode;
    }

    private LinkAsResponseDto convertActualLinkToResponseLink(Link link) {
        return LinkAsResponseDto.builder()
                .id(link.getId().toString())
                .actualUrl(link.getActualUrl())
                .status(link.getStatus())
                .hashedKey(link.getHashedKey())
                .clickCnt(link.getClickCount())
                .reportCnt(link.getReportCount())
                .creationTime(link.getLinkCreationTime().toInstant())
                .build();
    }

    public boolean incrementClickCountIfExists(String shortCode) {
        return mongoLinkService.incrementClickCountIfExists(shortCode);
    }
}
