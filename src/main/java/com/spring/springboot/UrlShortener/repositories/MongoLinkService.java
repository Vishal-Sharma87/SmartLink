package com.spring.springboot.UrlShortener.repositories;

import com.spring.springboot.UrlShortener.advices.exceptions.ResourceNotExistsException;
import com.spring.springboot.UrlShortener.entity.Links;
import com.spring.springboot.UrlShortener.enums.FinalVerdict;

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

    private final MongoTemplate mongoTemplate;
    private final LinkRepository linkRepository;


    String linkStatus = "status";
    String ownerUserName = "ownerUserName";

    private List<Links> getAllLinksOfAnUser(String userName) {

        Query query = new Query();
        query.addCriteria(Criteria.where(ownerUserName).is(userName));


        return mongoTemplate.find(query, Links.class);
    }

    private List<Links> getAllSafeLinksOfAnUser(String userName) {
        Query query = new Query();

        Criteria criteria = new Criteria().andOperator(
                Criteria.where(ownerUserName).is(userName),
                Criteria.where(linkStatus).is("SAFE")
        );
        query.addCriteria(criteria);

        return mongoTemplate.find(query, Links.class);
    }

    private List<Links> getAllUnsafeLinksOfAnUser(String userName) {
        Query query = new Query();

        Criteria criteria = new Criteria().andOperator(
                Criteria.where(ownerUserName).is(userName),
                Criteria.where(linkStatus).in(List.of("SUSPICIOUS", "MALICIOUS"))
        );
        query.addCriteria(criteria);

        return mongoTemplate.find(query, Links.class);
    }

    private void deleteAllLinksOfAnUser(String userName) {

        Query query = new Query();
        query.addCriteria(Criteria.where(ownerUserName).is(userName));


        List<Long> allLinkIdsToDelete = mongoTemplate.find(query, Links.class).stream().map(Links::getId).toList();
        linkRepository.deleteAllById(allLinkIdsToDelete);
    }

    private void deleteAllSafeLinksOfAnUser(String userName) {
        Query query = new Query();

        Criteria criteria = new Criteria().andOperator(
                Criteria.where(ownerUserName).is(userName),
                Criteria.where(linkStatus).is("SAFE")
        );
        query.addCriteria(criteria);

        List<Long> allSafeLinkIdsToDelete = mongoTemplate.find(query, Links.class).stream().map(Links::getId).toList();
        linkRepository.deleteAllById(allSafeLinkIdsToDelete);
    }

    private void deleteAllUnsafeLinksOfAnUser(String userName) {
        Query query = new Query();

        Criteria criteria = new Criteria().andOperator(
                Criteria.where(ownerUserName).is(userName),
                Criteria.where(linkStatus).in(List.of("SUSPICIOUS", "MALICIOUS"))
        );
        query.addCriteria(criteria);

        List<Long> allUnsafeLinkIdsToDelete = mongoTemplate.find(query, Links.class).stream().map(Links::getId).toList();
        linkRepository.deleteAllById(allUnsafeLinkIdsToDelete);
    }

    public List<Links> getAllLinksOfAnUserByType(String userName, String type) {
        return switch (type.toLowerCase()) {
            case "all" -> getAllLinksOfAnUser(userName);
            case "safe" -> getAllSafeLinksOfAnUser(userName);
            case "unsafe" -> getAllUnsafeLinksOfAnUser(userName);
            default -> throw new ResourceNotExistsException("No links to show or invalid query");
        };
    }


    public void deleteAllLinksOfAnUserByType(String userName, String type) {
        switch (type.toLowerCase()) {
            case "all" -> deleteAllLinksOfAnUser(userName);
            case "safe" -> deleteAllSafeLinksOfAnUser(userName);
            case "unsafe" -> deleteAllUnsafeLinksOfAnUser(userName);
            default -> throw new ResourceNotExistsException("No links to delete or invalid query");
        }
    }

    public Links getLinkOfAnUserById(String idToFind, String userName) {
        Query query = new Query();

        Criteria criteria = new Criteria().andOperator(
                Criteria.where("id").is(Long.parseLong(idToFind)),
                Criteria.where(ownerUserName).is(userName)
        );
        query.addCriteria(criteria);
        return mongoTemplate.find(query, Links.class).stream().findFirst().orElseThrow(() -> new ResourceNotExistsException("Link does not exists, id: " + idToFind));
    }

    public Links findLinkByHashedKey(String hashedKey) {
        Query query = new Query();

        query.addCriteria(Criteria.where("hashedKey").is(hashedKey));

        List<Links> links = mongoTemplate.find(query, Links.class);
        return links.getFirst();
    }

    public int incrementAndGetReportCount(String hashedKey) {
        Query query = Query.query(Criteria.where("hashedKey").is(hashedKey));
        Update update = new Update()
                .inc("reportCount", 1);
        Links updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Links.class
        );

        if (updated == null){
            throw new ResourceNotExistsException("Link with hashkey: " + hashedKey +" Not exists");
        }

        return updated.getReportCount();
    }

    public void updateStatus(String hashedKey, FinalVerdict.Verdict verdict) {
        Query query = Query.query(Criteria.where("hashedKey").is(hashedKey));
        Update update = new Update()
                .set("status", verdict);

        mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options(),
                Links.class
        );
    }
}
