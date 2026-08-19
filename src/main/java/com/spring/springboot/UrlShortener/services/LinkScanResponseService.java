package com.spring.springboot.UrlShortener.services;

import org.springframework.stereotype.Service;

import com.spring.springboot.UrlShortener.advices.exceptions.ResourceWithHashNotExistsException;
import com.spring.springboot.UrlShortener.entity.LinkScanResponse;
import com.spring.springboot.UrlShortener.repositories.LinkScanResponseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LinkScanResponseService {

    private final LinkScanResponseRepository linkScanResponseRepository;

    public void saveNew(LinkScanResponse linkScanResponse) {
        linkScanResponseRepository.save(linkScanResponse);
    }

    public LinkScanResponse getLinkScanResponse(String shortCode) {
        return linkScanResponseRepository
                .findById(shortCode)
                .orElseThrow(() -> new ResourceWithHashNotExistsException(
                        "Link Scan Details does not exists, shortCode: " + shortCode));
    }

}
