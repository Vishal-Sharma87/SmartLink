package com.spring.springboot.smartlink.analytics.services;

import org.springframework.stereotype.Service;

import com.spring.springboot.smartlink.advices.exceptions.LinkScanNotFoundExceptionSmartLink;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.analytics.entities.LinkScanResponse;
import com.spring.springboot.smartlink.analytics.repositories.LinkScanResponseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LinkScanResponseService {

    private final LinkScanResponseRepository linkScanResponseRepository;
    private final ExceptionMessages exceptionMessages;

    public void saveNew(LinkScanResponse linkScanResponse) {
        linkScanResponseRepository.save(linkScanResponse);
    }

    public LinkScanResponse getLinkScanResponse(String shortCode) {
        return linkScanResponseRepository
                .findById(shortCode)
                .orElseThrow(() -> new LinkScanNotFoundExceptionSmartLink(
                        String.format(exceptionMessages.linkScanNotFound(), shortCode)));
    }

}
