package com.spring.springboot.smartlink.link.controllers;

import com.spring.springboot.smartlink.apiresponse.ApiResponse;
import com.spring.springboot.smartlink.link.dtos.UrlToShortRequestDto;
import com.spring.springboot.smartlink.link.dtos.LinkAsResponseDto;
import com.spring.springboot.smartlink.link.dtos.LinkCreationResponseDto;
import com.spring.springboot.smartlink.link.dtos.LinkQueryResponseDto;
import com.spring.springboot.smartlink.analytics.entities.LinkScanResponse;
import com.spring.springboot.smartlink.link.services.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/link")
@RequiredArgsConstructor
public class LinkController {

        private final LinkService linkService;

        // Create
        @PostMapping("/create")
        public ResponseEntity<ApiResponse<LinkCreationResponseDto>> createNewShortLink(
                        @Valid @RequestBody UrlToShortRequestDto urlDto) {

                String userEmail = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName();

                LinkCreationResponseDto response = linkService.initializeCreation(urlDto.originalUrl(), userEmail);

                return ResponseEntity
                                .status(HttpStatus.ACCEPTED)
                                .body(ApiResponse.of(response));
        }

        @PostMapping("create/sync")
        public ResponseEntity<ApiResponse<LinkCreationResponseDto>> createNewShortLinkSynchronously(
                        @Valid @RequestBody UrlToShortRequestDto urlDto) {

                String userEmail = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName();

                LinkCreationResponseDto response = linkService.initializeCreationSync(urlDto.originalUrl(), userEmail);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.of(response));
        }

        // Read all
        @GetMapping("/")
        public ResponseEntity<ApiResponse<LinkQueryResponseDto>> getAllLinksOfAnUser() {

                String userEmail = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName();

                LinkQueryResponseDto response = linkService.getAllLinksOfAnUser(userEmail);

                return ResponseEntity
                                .status(HttpStatus.OK)
                                .body(ApiResponse.of(response));
        }

        // Read one
        @GetMapping("/{hash}")
        public ResponseEntity<ApiResponse<LinkAsResponseDto>> getLinkByHash(
                        @PathVariable String hash) {

                String userEmail = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName();

                LinkAsResponseDto linkResponse = linkService.findLinkOfUser(hash, userEmail);

                return ResponseEntity
                                .status(HttpStatus.OK)
                                .body(ApiResponse.of(linkResponse));
        }

        @DeleteMapping("/{hash}")
        public ResponseEntity<ApiResponse<String>> deleteLinkByHash(
                        @PathVariable String hash) {

                String userEmail = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName();

                String response = linkService.deleteLinkOfUser(userEmail, hash);

                return ResponseEntity
                                .status(HttpStatus.OK)
                                .body(ApiResponse.of(response));
        }

        // Delete all
        @DeleteMapping("/")
        public ResponseEntity<String> deleteAllLinksOfAnUser() {

                String userEmail = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName();

                String response = linkService.deleAllLinkOfUser(userEmail);

                return ResponseEntity
                                .status(HttpStatus.OK)
                                .body(response);
        }

        @GetMapping("/debug/verdict/{shortCode}")
        public ResponseEntity<ApiResponse<LinkScanResponse>> getLinkScanDetails(
                        @PathVariable String shortCode) {

                LinkScanResponse details = linkService.getLinkScanDetails(shortCode);

                return ResponseEntity
                                .ok(ApiResponse.of(details));
        }
}
