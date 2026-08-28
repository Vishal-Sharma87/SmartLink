package com.spring.springboot.smartlink.services;

import com.spring.springboot.smartlink.advices.exceptions.ResourceNotExistsException;
import com.spring.springboot.smartlink.dto.requestDtos.ReportLinkRequestDto;
import com.spring.springboot.smartlink.entity.AbuseReport;
import com.spring.springboot.smartlink.entity.Link;

import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.repositories.LinkRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MongoLinkService {

    private static final String ID = "id";
    private static final String HASHED_KEY = "hashedKey";
    private static final String OWNER_USER_NAME = "ownerUserName";
    private static final String STATUS =  "status";
    private static final String REPORT_COUNT = "reportCount";
    private static final String REPORTER_EMAIL = "reporterEmail";

    private final MongoTemplate mongoTemplate;
    private final LinkRepository linkRepository;


    public List<Link> getAllLinksOfAnUser(String userName) {
        Query query = new Query();
        query.addCriteria(Criteria.where(OWNER_USER_NAME).is(userName));

        return mongoTemplate.find(query, Link.class);
    }

    public void deleteAllLinksOfAnUser(String userName) {

        Query query = new Query();
        query.addCriteria(Criteria.where(OWNER_USER_NAME).is(userName));


        List<Long> allLinkIdsToDelete = mongoTemplate.find(query, Link.class).stream().map(Link::getId).toList();
        linkRepository.deleteAllById(allLinkIdsToDelete);
    }


    public Link getLinkOfAnUserById(String idToFind, String userName) {
        Query query = new Query();

        Criteria criteria = new Criteria().andOperator(
                Criteria.where(ID).is(Long.parseLong(idToFind)),
                Criteria.where(OWNER_USER_NAME).is(userName)
        );
        query.addCriteria(criteria);
        return mongoTemplate.find(query, Link.class).stream().findFirst().orElseThrow(() -> new ResourceNotExistsException("Link does not exists, id: " + idToFind));
    }

    public int incrementAndGetReportCount(String hashedKey) {
        Query query = Query.query(Criteria.where(HASHED_KEY).is(hashedKey));
        Update update = new Update()
                .inc(REPORT_COUNT, 1);
        Link updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Link.class
        );

        if (updated == null){
            throw new ResourceNotExistsException("Link with hashkey: " + hashedKey +" Not exists");
        }

        return updated.getReportCount();
    }

    public void updateStatus(String hashedKey, Verdict verdict) {
        Query query = Query.query(Criteria.where(HASHED_KEY).is(hashedKey));
        Update update = new Update()
                .set(STATUS, verdict);

        mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options(),
                Link.class
        );
    }

    public void deleteLinkOfUserById(Long idToDelete, String userName) {
        Query query = new Query();
        query.addCriteria(
                new Criteria().andOperator(
                        Criteria.where(ID).is(idToDelete),
                        Criteria.where(OWNER_USER_NAME).is(userName)
                )
        );
        
        mongoTemplate.findAndRemove(query, Link.class);
    }

    public boolean isAlreadyReported(String hashedKey, @Valid ReportLinkRequestDto dto) {
        Query query = new Query();
        Criteria criteria = new Criteria().andOperator(
                Criteria.where(HASHED_KEY).is(hashedKey),
                Criteria.where(REPORTER_EMAIL).is(dto.getReporterEmail())

        );

        query.addCriteria(criteria);

//        we will stop finding in db when we find first report entry
        query.limit(1);

        return !mongoTemplate.find(query, AbuseReport.class).isEmpty();
    }
}
