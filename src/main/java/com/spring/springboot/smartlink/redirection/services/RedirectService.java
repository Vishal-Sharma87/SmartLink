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

    private static final String MODEL_ATTRIBUTE_LONG_URL = "longUrl";
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
        if (linkService.isAfterLastCounter(Base62.decode(shortCode, exceptionMessages.invalidLinkHash()))) {
            return redirectionPageConfigs.errorPage();
        }

        String longUrl;

        List<Object> cachedData = redisService.getRedirectionCache(shortCode);

        if (cachedData != null && cachedData.getFirst() != null && cachedData.getLast() != null) {
            longUrl = cachedData.getFirst().toString();
        } else {
            Link linkInDb = linkService.getLinkByHash(shortCode);

            if (linkInDb == null) {
                log.warn("Link with shortCode {} not found", shortCode);
                return redirectionPageConfigs.errorPage();
            }

            longUrl = linkInDb.getActualUrl();
            redisService.putInRedirectionCache(shortCode, longUrl, linkInDb.getStatus());
        }

        model.addAttribute(MODEL_ATTRIBUTE_LONG_URL, longUrl);
        model.addAttribute(MODEL_ATTRIBUTE_SHORT_CODE, shortCode);

        return redirectionPageConfigs.trackPage();
    }

    public String addModelAttributesAndGetPageToServeString(String shortCode, Model model) {
        if (linkService.isAfterLastCounter(Base62.decode(shortCode, exceptionMessages.invalidLinkHash()))) {
            return redirectionPageConfigs.errorPage();
        }

        String longUrl;
        Verdict linkStatus;

        List<Object> cachedData = redisService.getRedirectionCache(shortCode);

        if (cachedData != null && cachedData.getFirst() != null && cachedData.getLast() != null) {
            longUrl = cachedData.getFirst().toString();
            linkStatus = Verdict.valueOf(cachedData.getLast().toString());
        } else {
            Link linkInDb = linkService.getLinkByHash(shortCode);

            if (linkInDb == null) {
                return redirectionPageConfigs.errorPage(); // HTML page "error"
            }
            longUrl = linkInDb.getActualUrl();
            linkStatus = linkInDb.getStatus();

            redisService.putInRedirectionCache(shortCode, longUrl, linkStatus);
        }

        model.addAttribute(MODEL_ATTRIBUTE_LONG_URL, longUrl);
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
