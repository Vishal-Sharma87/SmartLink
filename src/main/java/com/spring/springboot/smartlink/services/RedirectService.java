package com.spring.springboot.smartlink.services;

import com.spring.springboot.smartlink.advices.exceptions.RedirectionHashInvalidException;
import com.spring.springboot.smartlink.entity.Link;
import com.spring.springboot.smartlink.dto.RedirectServiceResponseDto;
import com.spring.springboot.smartlink.enums.Verdict;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedirectService {
    private static final String REDIS_REDIRECTION_CACHE_PREFIX = "smart-link:redirection-hash:";

    private static final String REDIRECTION_CACHE_STATUS_HASH_KEY = "status";
    private static final String REDIRECTION_CACHE_LONG_URL_HASH_KEY = "longUrl";

    private static final String MODEL_ATTRIBUTE_LONG_URL = "longUrl";
    private static final String MODEL_ATTRIBUTE_SHORT_CODE = "shortCode";

    private final RedisService redisService;
    private final LinkService linkService;

    public RedirectServiceResponseDto getActualUrlIfExists(String hash) {
        List<Object> cachedData = getCachedData(hash);

        if (cachedData != null){
            String longUrl = cachedData.getFirst().toString();
            Verdict status = Verdict.valueOf(cachedData.getLast().toString());

            return RedirectServiceResponseDto.builder()
                    .longUrl(longUrl)
                    .status(status)
                    .build();

        }

        Link linkInDb = linkService.getLinkByHash(hash);

        if (linkInDb == null) {
            log.warn("Link with hash {} not found", hash);
            return null;
        }

        putInRedirectionCache(hash, linkInDb);

        return RedirectServiceResponseDto.builder()
                .longUrl(linkInDb.getActualUrl())
                .status(linkInDb.getStatus())
                .build();
    }

    public String putModelAttributesIfLinkExists(String hash, Model model) {
        String longUrl;
        Verdict linkStatus;

        List<Object> cachedData = getCachedData(hash);

        if (cachedData != null && cachedData.getFirst() != null && cachedData.getLast() != null){
            longUrl = cachedData.getFirst().toString();
            linkStatus = Verdict.valueOf(cachedData.getLast().toString());
        }else{
            Link linkInDb = linkService.getLinkByHash(hash);

            if (linkInDb == null) {
                return "error"; // HTML page "error"
            }

            putInRedirectionCache(hash, linkInDb);

            longUrl = linkInDb.getActualUrl();
            linkStatus = linkInDb.getStatus();
        }

        model.addAttribute(MODEL_ATTRIBUTE_LONG_URL, longUrl);
        model.addAttribute(MODEL_ATTRIBUTE_SHORT_CODE, hash);

        return switch (linkStatus) {
            case Verdict.SAFE:
                // Direct redirect
                log.info("Safe short URL {} prepared for redirection", hash);
                yield "track";

            case Verdict.SUSPICIOUS, Verdict.PENDING_REVERIFICATION,
                 Verdict.UNVERIFIED:
                log.warn("Short URL with hash {} requires a safety warning before redirection with status {}", hash, linkStatus);
                yield  "suspicious-warning";

            case Verdict.MALICIOUS:
                // Show blocked page
                log.warn("Blocked redirection for malicious short URL {}", hash);
                yield  "malicious-warning";
        };
    }

    private void putInRedirectionCache(String hash, Link link) {
        String cacheKey = buildCacheKey(hash);

        Map<String, String> cache = new HashMap<>();
        cache.put(REDIRECTION_CACHE_LONG_URL_HASH_KEY, link.getActualUrl());
        cache.put(REDIRECTION_CACHE_STATUS_HASH_KEY,link.getStatus().name());

        redisService.putInRedirectionCache(cacheKey, cache);
    }

    private List<Object> getCachedData(String hash) {
        if (linkService.isAfterLastCounter(Base62.decode(hash))){
            throw new RedirectionHashInvalidException("Invalid shortCode: " + hash);
        }

        String cacheKey = buildCacheKey(hash);

        List<Object> cacheHashKeys = List.of(
                REDIRECTION_CACHE_LONG_URL_HASH_KEY,
                REDIRECTION_CACHE_STATUS_HASH_KEY
        );

        return redisService.getCachedDataByKey(cacheKey, cacheHashKeys);
    }

    private String buildCacheKey(String hash) {
        return REDIS_REDIRECTION_CACHE_PREFIX + hash;
    }
}
