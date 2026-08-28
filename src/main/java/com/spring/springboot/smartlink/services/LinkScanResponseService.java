package com.spring.springboot.smartlink.services;

import org.springframework.stereotype.Service;

import com.spring.springboot.smartlink.advices.exceptions.ResourceWithHashNotExistsException;
import com.spring.springboot.smartlink.entity.LinkScanResponse;
import com.spring.springboot.smartlink.repositories.LinkScanResponseRepository;

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
