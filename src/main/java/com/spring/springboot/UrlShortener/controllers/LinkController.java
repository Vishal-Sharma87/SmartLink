package com.spring.springboot.UrlShortener.controllers;

import com.spring.springboot.UrlShortener.advices.exceptions.ResourceNotExistsException;
import com.spring.springboot.UrlShortener.dto.UrlToShortRequestDto;
import com.spring.springboot.UrlShortener.dto.responseDtos.LinkAsResponseDto;
import com.spring.springboot.UrlShortener.dto.responseDtos.LinkCreationResponseDto;
import com.spring.springboot.UrlShortener.dto.responseDtos.LinkQueryResponseDto;
import com.spring.springboot.UrlShortener.entity.LinkScanResponse;
import com.spring.springboot.UrlShortener.entity.Links;
import com.spring.springboot.UrlShortener.services.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/link")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    // Create
    @PostMapping("/create")
    public ResponseEntity<LinkCreationResponseDto> createNewShortLink(@RequestBody UrlToShortRequestDto urlDto) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();

        // a method to create a working url from a urlDto
        String shortUrl = linkService.generateShortUrl(urlDto, userName);

        // creating a response to return
        LinkCreationResponseDto response = LinkCreationResponseDto
                .builder()
                .shortUrl(shortUrl)
                .message("Short URL created and queued for processing.")
                .status("PROCESSING")
                .build();

        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @PostMapping("create/sync")
    public ResponseEntity<LinkCreationResponseDto> createNewShortLinkSynchronously(
            @RequestBody UrlToShortRequestDto urlDto) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();

        // a method to create a working url from a urlDto
        String shortUrl = linkService.generateShortUrlSync(urlDto, userName);

        // creating a response to return
        LinkCreationResponseDto response = LinkCreationResponseDto
                .builder()
                .shortUrl(shortUrl)
                .message("Short URL created and working")
                .status("CREATED")
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // read all
    @GetMapping("/get-all-links")
    public ResponseEntity<LinkQueryResponseDto<List<LinkAsResponseDto>>> getAllLinksOfAnUser(
            @RequestParam(defaultValue = "all") String type) {
        try {
            String userName = SecurityContextHolder.getContext().getAuthentication().getName();

            // calling getAllLinksOfAnUser method of linkService which expect type of links
            // to
            // be provided from path parameter,
            // default is all -> safe + sus + mal,
            // safe -> safe,
            // unsafe -> malicious + suspicious
            // else -> invalid query
            List<LinkAsResponseDto> allLinksOfAnUser = linkService.getAllLinksOfAnUser(userName, type);

            return ResponseEntity.ok(new LinkQueryResponseDto<>(allLinksOfAnUser, "Content found", new Date()));

        } catch (ResourceNotExistsException e) {
            return new ResponseEntity<>(new LinkQueryResponseDto<>(null, e.getMessage(), new Date()),
                    HttpStatus.NOT_FOUND);
        }
    }

    // read one
    @GetMapping("/get-link")
    public ResponseEntity<LinkAsResponseDto> getLinkByIdOrHashedKey(@RequestParam() String by,
            @RequestParam() String value) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();

        LinkAsResponseDto linkResponse = linkService.findLink(userName, by, value);
        return new ResponseEntity<>(linkResponse, HttpStatus.OK);
    }

    // delete by id/hash
    @DeleteMapping("/delete-link")
    public ResponseEntity<Links> deleteLinkByIdOrHashedKey(@RequestParam() String by, @RequestParam() String value) {

        linkService.deleteLink(by, value);
        return new ResponseEntity<>(HttpStatus.OK);
        // return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // delete all
    @DeleteMapping("/delete-all-links")
    public ResponseEntity<String> deleteAllLinksOfAnUser(@RequestParam(defaultValue = "all") String type) {

        String userName = SecurityContextHolder.getContext().getAuthentication().getName();

        linkService.deleteAllLinksOfAnUserByType(userName, type);

        return new ResponseEntity<>(type + " links are deleted from database", HttpStatus.OK);
    }

    @GetMapping("/debug/verdict/{shortCode}")
    public ResponseEntity<LinkScanResponse> getLinkScanDetails(@PathVariable String shortCode) {

        LinkScanResponse details = linkService.getLinkScanDetails(shortCode);
        
        return ResponseEntity.ok(details);
    }

}
