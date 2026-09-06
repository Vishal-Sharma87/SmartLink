package com.spring.springboot.smartlink.link.controllers;

import com.spring.springboot.smartlink.apiresponse.ApiResponse;
import com.spring.springboot.smartlink.link.dtos.UrlToShortRequestDto;
import com.spring.springboot.smartlink.link.dtos.LinkAsResponseDto;
import com.spring.springboot.smartlink.link.dtos.LinkCreationResponseDto;
import com.spring.springboot.smartlink.link.dtos.LinkQueryResponseDto;
import com.spring.springboot.smartlink.analytics.entities.LinkScanResponse;
import com.spring.springboot.smartlink.link.services.LinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/link")
@RequiredArgsConstructor
@Slf4j
public class LinkController {

        private final LinkService linkService;

        // Create
        @PostMapping("/create")
        public ResponseEntity<ApiResponse<LinkCreationResponseDto>> createNewShortLink(
                        @RequestBody UrlToShortRequestDto urlDto) {

                String userName = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName();

                log.info("Request received to create short URL asynchronously for user {}", userName);
                log.debug("Actual URL received for async creation: {}", urlDto.getActualUrl());

                LinkCreationResponseDto response = linkService.initializeCreation(urlDto.getActualUrl(), userName);

                return ResponseEntity
                                .status(HttpStatus.ACCEPTED)
                                .body(ApiResponse.of(response));
        }

        @PostMapping("create/sync")
        public ResponseEntity<ApiResponse<LinkCreationResponseDto>> createNewShortLinkSynchronously(
                        @RequestBody UrlToShortRequestDto urlDto) {

                String userName = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName();

                log.info("Request received to create short URL synchronously for user {}", userName);
                log.debug("Actual URL received for sync creation: {}", urlDto.getActualUrl());

                LinkCreationResponseDto response = linkService.initializeCreationSync(urlDto.getActualUrl(), userName);

                log.info("Short URL created synchronously for user {}", userName);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.of(response));
        }

        // Read all
        @GetMapping("/")
        public ResponseEntity<ApiResponse<LinkQueryResponseDto>> getAllLinksOfAnUser() {

                String userName = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName();

                log.info("Request received to fetch all links for user {}", userName);

                LinkQueryResponseDto response = linkService.getAllLinksOfAnUser(userName);

                return ResponseEntity
                                .status(HttpStatus.OK)
                                .body(ApiResponse.of(response));
        }

        // Read one
        @GetMapping("/{hash}")
        public ResponseEntity<ApiResponse<LinkAsResponseDto>> getLinkByHash(
                        @PathVariable String hash) {

                String userName = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName();

                log.info("Request received to fetch link with hash {} for user {}", hash, userName);

                LinkAsResponseDto linkResponse = linkService.findLinkOfUser(hash, userName);

                log.info("Link with hash {} retrieved successfully for user {}", hash, userName);

                return ResponseEntity
                                .status(HttpStatus.OK)
                                .body(ApiResponse.of(linkResponse));
        }

        @DeleteMapping("/{hash}")
        public ResponseEntity<ApiResponse<String>> deleteLinkByHash(
                        @PathVariable String hash) {

                String userName = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName();

                log.info("Request received to delete link with hash {} for user {}", hash, userName);

                String response = linkService.deleteLinkOfUser(userName, hash);

                log.info("Link with hash {} deleted successfully for user {}", hash, userName);

                return ResponseEntity
                                .status(HttpStatus.OK)
                                .body(ApiResponse.of(response));
        }

        // Delete all
        @DeleteMapping("/")
        public ResponseEntity<String> deleteAllLinksOfAnUser() {

                String userName = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName();

                log.info("Request received to delete all links for user {}", userName);

                String response = linkService.deleAllLinkOfUser(userName);

                log.info("All links deleted successfully for user {}", userName);

                return ResponseEntity
                                .status(HttpStatus.OK)
                                .body(response);
        }

        @GetMapping("/debug/verdict/{shortCode}")
        public ResponseEntity<ApiResponse<LinkScanResponse>> getLinkScanDetails(
                        @PathVariable String shortCode) {

                log.info("Request received to retrieve scan details for short code {}", shortCode);

                LinkScanResponse details = linkService.getLinkScanDetails(shortCode);

                log.debug("Retrieved scan details for short code {}", shortCode);

                return ResponseEntity
                                .ok(ApiResponse.of(details));
        }
}