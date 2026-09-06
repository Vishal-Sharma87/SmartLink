package com.spring.springboot.smartlink.analytics.services;

import com.spring.springboot.smartlink.analytics.entities.LinkInformation;
import com.spring.springboot.smartlink.analytics.repositories.LinkInformationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkInformationService {

    private final LinkInformationRepository linkInformationRepository;

    public void save(LinkInformation linkInformation) {
        linkInformationRepository.save(linkInformation);
    }

    public List<LinkInformation> findByShortHash(String shortHash) {
        return linkInformationRepository.findAllByAssociatedShortHashOrderByTimeOfClickDesc(shortHash);
    }

}
