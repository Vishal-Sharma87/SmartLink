package com.spring.springboot.smartlink.redirection.services;

import com.spring.springboot.smartlink.configurations.RedirectionPageConfigs;
import com.spring.springboot.smartlink.link.entities.Link;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.redis.services.RedisService;
import com.spring.springboot.smartlink.utils.Base62;
import com.spring.springboot.smartlink.link.services.LinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.*;

@Slf4j
@Service
public class RedirectService {

    private static final String MODEL_ATTRIBUTE_LONG_URL = "originalUrl";
    private static final String MODEL_ATTRIBUTE_SHORT_CODE = "shortCode";

    private final RedisService redisService;
    private final LinkService linkService;
    private final ExceptionMessages exceptionMessages;
    private final RedirectionPageConfigs redirectionPageConfigs;

    public RedirectService(
            RedisService redisService,
            LinkService linkService,
            ExceptionMessages exceptionMessages,
            RedirectionPageConfigs redirectionPageConfigs) {

        this.redisService = redisService;
        this.linkService = linkService;
        this.exceptionMessages = exceptionMessages;
        this.redirectionPageConfigs = redirectionPageConfigs;
    }

    public String addAttributeAndGetPageToReturn(String shortCode, Model model) {
        if (linkService.isNotInCounterRange(Base62.decode(shortCode, exceptionMessages.invalidLinkHash()))) {
            return redirectionPageConfigs.errorPage();
        }

        String originalUrl;

        List<String> cachedData = redisService.getRedirectionCache(shortCode);

        if (cachedData != null && cachedData.getFirst() != null && cachedData.getLast() != null) {
            originalUrl = cachedData.getFirst();
        } else {
            Link linkInDb = linkService.getLinkByShortCode(shortCode);

            if (linkInDb == null) {
                log.warn("Link with shortCode {} not found", shortCode);
                return redirectionPageConfigs.errorPage();
            }

            originalUrl = linkInDb.getOriginalUrl();
            redisService.putInRedirectionCache(shortCode, originalUrl, linkInDb.getStatus());
        }

        model.addAttribute(MODEL_ATTRIBUTE_LONG_URL, originalUrl);
        model.addAttribute(MODEL_ATTRIBUTE_SHORT_CODE, shortCode);

        return redirectionPageConfigs.trackPage();
    }

    public String addModelAttributesAndGetPageToServeString(String shortCode, Model model) {
        if (linkService.isNotInCounterRange(Base62.decode(shortCode, exceptionMessages.invalidLinkHash()))) {
            return redirectionPageConfigs.errorPage();
        }

        String originalUrl;
        Verdict linkStatus;

        List<String> cachedData = redisService.getRedirectionCache(shortCode);

        if (cachedData != null && cachedData.getFirst() != null && cachedData.getLast() != null) {
            originalUrl = cachedData.getFirst();
            linkStatus = Verdict.valueOf(cachedData.getLast());
        } else {
            Link linkInDb = linkService.getLinkByShortCode(shortCode);

            if (linkInDb == null) {
                return redirectionPageConfigs.errorPage(); // HTML page "error"
            }
            originalUrl = linkInDb.getOriginalUrl();
            linkStatus = linkInDb.getStatus();

            redisService.putInRedirectionCache(shortCode, originalUrl, linkStatus);
        }

        model.addAttribute(MODEL_ATTRIBUTE_LONG_URL, originalUrl);
        model.addAttribute(MODEL_ATTRIBUTE_SHORT_CODE, shortCode);

        return switch (linkStatus) {
            case Verdict.SAFE:
                // Direct redirect
                log.info("Safe short URL {} prepared for redirection", shortCode);
                yield redirectionPageConfigs.trackPage();

            case Verdict.SUSPICIOUS, Verdict.PENDING_REVERIFICATION,
                    Verdict.UNVERIFIED:
                log.warn("Short URL with shortCode {} requires a safety warning before redirection with status {}",
                        shortCode, linkStatus);
                yield redirectionPageConfigs.suspiciousWarningPage();

            case Verdict.MALICIOUS:
                // Show blocked page
                log.warn("Blocked redirection for malicious short URL {}", shortCode);
                yield redirectionPageConfigs.maliciousPage();
        };
    }

}
