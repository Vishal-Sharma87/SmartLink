package com.spring.springboot.smartlink.controllers;

import com.spring.springboot.smartlink.dto.UrlToShortRequestDto;
import com.spring.springboot.smartlink.dto.responseDtos.LinkAsResponseDto;
import com.spring.springboot.smartlink.dto.responseDtos.LinkCreationResponseDto;
import com.spring.springboot.smartlink.dto.responseDtos.LinkQueryResponseDto;
import com.spring.springboot.smartlink.entity.Link;
import com.spring.springboot.smartlink.entity.LinkScanResponse;
import com.spring.springboot.smartlink.services.LinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/link")
@RequiredArgsConstructor
@Slf4j
public class LinkController {

    private final LinkService linkService;

    // Create
    @PostMapping("/create")
    public ResponseEntity<LinkCreationResponseDto> createNewShortLink(
            @RequestBody UrlToShortRequestDto urlDto) {

        String userName = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        log.info("Request received to create short URL asynchronously for user {}", userName);
        log.debug("Actual URL received for async creation: {}", urlDto.getActualUrl());

        LinkCreationResponseDto response =
                linkService.initializeCreation(urlDto.getActualUrl(), userName);

        log.info("Short URL creation queued successfully for user {}", userName);

        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @PostMapping("create/sync")
    public ResponseEntity<LinkCreationResponseDto> createNewShortLinkSynchronously(
            @RequestBody UrlToShortRequestDto urlDto) {

        String userName = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        log.info("Request received to create short URL synchronously for user {}", userName);
        log.debug("Actual URL received for sync creation: {}", urlDto.getActualUrl());

        LinkCreationResponseDto response =
                linkService.initializeCreationSync(urlDto.getActualUrl(), userName);

        log.info("Short URL created synchronously for user {}", userName);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Read all
    @GetMapping("/")
    public ResponseEntity<LinkQueryResponseDto<List<LinkAsResponseDto>>> getAllLinksOfAnUser() {

        String userName = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        log.info("Request received to fetch all links for user {}", userName);

        List<LinkAsResponseDto> allLinksOfAnUser =
                linkService.getAllLinksOfAnUser(userName);

        log.info("Retrieved {} links for user {}", allLinksOfAnUser.size(), userName);

        return ResponseEntity.ok(
                new LinkQueryResponseDto<>(
                        allLinksOfAnUser,
                        "Content found",
                        new Date()
                )
        );
    }

    // Read one
    @GetMapping("/{hash}")
    public ResponseEntity<LinkAsResponseDto> getLinkByHash(
            @PathVariable String hash) {

        String userName = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        log.info("Request received to fetch link with hash {} for user {}", hash, userName);

        LinkAsResponseDto linkResponse =
                linkService.findLinkByHash(hash, userName);

        log.info("Link with hash {} retrieved successfully for user {}", hash, userName);

        return new ResponseEntity<>(linkResponse, HttpStatus.OK);
    }

    @DeleteMapping("/{hash}")
    public ResponseEntity<Link> deleteLinkByHash(
            @PathVariable String hash) {

        String userName = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        log.info("Request received to delete link with hash {} for user {}", hash, userName);

        linkService.deleteLinkOfUser(userName, hash);

        log.info("Link with hash {} deleted successfully for user {}", hash, userName);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    // Delete all
    @DeleteMapping("/")
    public ResponseEntity<String> deleteAllLinksOfAnUser() {

        String userName = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        log.info("Request received to delete all links for user {}", userName);

        linkService.deleAllLinkOfUser(userName);

        log.info("All links deleted successfully for user {}", userName);

        return new ResponseEntity<>(
                "All Links are deleted from database",
                HttpStatus.OK
        );
    }

    @GetMapping("/debug/verdict/{shortCode}")
    public ResponseEntity<LinkScanResponse> getLinkScanDetails(
            @PathVariable String shortCode) {

        log.info("Request received to retrieve scan details for short code {}", shortCode);

        LinkScanResponse details =
                linkService.getLinkScanDetails(shortCode);

        log.debug("Retrieved scan details for short code {}", shortCode);

        return ResponseEntity.ok(details);
    }
}