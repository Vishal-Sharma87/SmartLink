package com.spring.springboot.smartlink.services;

import com.spring.springboot.smartlink.advices.exceptions.NoSuchLinkExists;
import com.spring.springboot.smartlink.dto.responseDtos.LinkAsResponseDto;
import com.spring.springboot.smartlink.dto.responseDtos.LinkCreationResponseDto;
import com.spring.springboot.smartlink.entity.Link;
import com.spring.springboot.smartlink.entity.LinkScanResponse;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.model.LinkCreationDto;
import com.spring.springboot.smartlink.repositories.LinkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkService {


    private static final String LINK_CREATION_TOPIC = "smart_link_link_creation";
    private final LinkRepository linkRepository;
    private final RedisService redisService;
    private final MongoLinkService mongoLinkService;
    private final KafkaTemplate<String, LinkCreationDto> kafkaTemplate;
    private final LinkScanResponseService linkScanResponseService;

    @Value("${spring.link-creation.company-endpoint}")
    private String companyEndpoint;

    // just to test synchronous link creation
    private final VirusTotalService virusTotalService;

    public List<LinkAsResponseDto> getAllLinksOfAnUser(String userName) {
        List<Link> allLinkOfAnUserByType = mongoLinkService.getAllLinksOfAnUser(userName);
        return allLinkOfAnUserByType
                .stream()
                .map(this::convertActualLinkToResponseLink)
                .toList();

    }

    public boolean isAfterLastCounter(Long counterToCheck) {
        return redisService.isAfter(counterToCheck);
    }

    public LinkCreationResponseDto initializeCreation(String urlToShort, String userName) {
        Long urlCounter = redisService.getUrlCounter();

        String generatedHash = Base62.encode(urlCounter);

        LinkCreationDto dtoToCreate = LinkCreationDto.builder()
                .generatedHash(generatedHash)
                .id(urlCounter)
                .longUrl(urlToShort)
                .ownerUserName(userName)
                .build();

        kafkaTemplate.send(LINK_CREATION_TOPIC, dtoToCreate);
        log.info("Queued asynchronous link creation for short code {} and user {}", generatedHash, userName);

        String shortUrl = buildShortUrl(generatedHash);
        return  LinkCreationResponseDto
                .builder()
                .shortUrl(shortUrl)
                .message("Short URL created and queued for processing.")
                .status("PROCESSING")
                .build();
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

        String generatedHash = Base62.encode(urlCounter);

        Mono<Verdict> verdict = virusTotalService.scanUrl(urlToShort, generatedHash);

        save(Link.builder()
                .id(urlCounter)
                .actualUrl(urlToShort)
                .hashedKey(generatedHash)
                .linkCreationTime(new Date())
                .status(verdict.block())
                .ownerUserName(userName)
                .abuseReports(new ArrayList<>())
                .firstReportedTime(null)
                .lastReportedTime(null)
                .reportCount(0)
                .clickCount(0)
                .build());

        log.info("Synchronous link creation completed for short code {} and user {} with verdict {}", generatedHash,
                userName, verdict.block());

        String shortUrl = buildShortUrl(generatedHash);

        return LinkCreationResponseDto
                .builder()
                .shortUrl(shortUrl)
                .message("Short URL created and working")
                .status("CREATED")
                .build();
    }

    private String buildShortUrl(String generatedHash) {
        if (companyEndpoint == null || companyEndpoint.isBlank()) {
            return generatedHash;
        }
        return companyEndpoint.replaceAll("/+$", "") + "/" + generatedHash;
    }

    public void save(Link createdLink) {
        linkRepository.save(createdLink);
        log.debug("Saved link with short code {} and verdict {}", createdLink.getHashedKey(), createdLink.getStatus());
    }

    public LinkAsResponseDto findLinkByHash(String hash, String userName) {
        Long idToFind = Base62.decode(hash);

        if (hash.isEmpty() || isAfterLastCounter(idToFind))
            throw new NoSuchLinkExists("Link with hash %s not exists".formatted(hash));

        Link link = mongoLinkService.getLinkOfAnUserById(String.valueOf(idToFind), userName);
        return convertActualLinkToResponseLink(link);
    }


    public Link getLinkByHash(String hash) {
        Link link = linkRepository.findById(Base62.decode(hash)).orElse(null);
        if (link == null) {
            log.debug("No link found for short code {} during analytics processing", hash);
        }
        return link;
    }

    public LinkScanResponse getLinkScanDetails(String shortCode) {
        return linkScanResponseService.getLinkScanResponse(shortCode);
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


    public void deleteLinkOfUser(String userName, String hash) {
        Long idToDelete = Base62.decode(hash);

        if (hash.isEmpty() || isAfterLastCounter(idToDelete))
            throw new NoSuchLinkExists("Link with hash %s not exists".formatted(hash));

        mongoLinkService.deleteLinkOfUserById(idToDelete, userName);
    }

    public void deleAllLinkOfUser(String userName) {
        mongoLinkService.deleteAllLinksOfAnUser(userName);
    }
}
