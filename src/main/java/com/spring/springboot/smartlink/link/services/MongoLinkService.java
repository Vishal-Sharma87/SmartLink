package com.spring.springboot.smartlink.link.services;

import com.spring.springboot.smartlink.advices.exceptions.LinkNotFoundExceptionSmartLink;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.link.configs.LinkKeys;
import com.spring.springboot.smartlink.link.entities.Link;

import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.link.repositories.LinkRepository;
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
public class MongoLinkService {

        private final MongoTemplate mongoTemplate;
        private final LinkRepository linkRepository;
        private final ExceptionMessages exceptionMessages;
        private final LinkKeys linkKeys;

        public MongoLinkService(MongoTemplate mongoTemplate,
                        LinkRepository linkRepository,
                        ExceptionMessages exceptionMessages,
                        LinkKeys linkKeys) {

                this.mongoTemplate = mongoTemplate;
                this.linkRepository = linkRepository;
                this.exceptionMessages = exceptionMessages;
                this.linkKeys = linkKeys;
        }

        public List<Link> getAllLinksOfAnUser(String userName) {
                Query query = new Query();
                query.addCriteria(Criteria.where(linkKeys.ownerUserName()).is(userName));

                return mongoTemplate.find(query, Link.class);
        }

        public void deleteAllLinksOfAnUser(String userName) {

                Query query = new Query();
                query.addCriteria(Criteria.where(linkKeys.ownerUserName()).is(userName));

                List<Long> allLinkIdsToDelete = mongoTemplate.find(query, Link.class).stream().map(Link::getId)
                                .toList();
                linkRepository.deleteAllById(allLinkIdsToDelete);
        }

        public Link getLinkOfAnUserById(String idToFind, String userName) {
                Query query = new Query();

                Criteria criteria = new Criteria().andOperator(
                                Criteria.where(linkKeys.id()).is(Long.parseLong(idToFind)),
                                Criteria.where(linkKeys.ownerUserName()).is(userName));
                query.addCriteria(criteria);
                // The query proves that the requested link is absent for this owner; it does
                // not prove that the user is absent.
                return mongoTemplate.find(query, Link.class).stream().findFirst()
                                .orElseThrow(() -> new LinkNotFoundExceptionSmartLink(
                                                String.format(exceptionMessages.linkNotFound(), idToFind)));
        }

        public int incrementAndGetReportCount(String shortCode) {
                Query query = Query.query(Criteria.where(linkKeys.hashedKey()).is(shortCode));
                Update update = new Update()
                                .inc(linkKeys.reportCount(), 1);
                Link updated = mongoTemplate.findAndModify(
                                query,
                                update,
                                FindAndModifyOptions.options().returnNew(true),
                                Link.class);

                if (updated == null) {
                        throw new LinkNotFoundExceptionSmartLink(
                                        String.format(exceptionMessages.linkNotFound(), shortCode));
                }

                return updated.getReportCount();
        }

        public void updateStatus(String hashedKey, Verdict verdict) {
                Query query = Query.query(Criteria.where(linkKeys.hashedKey()).is(hashedKey));
                Update update = new Update()
                                .set(linkKeys.status(), verdict);

                mongoTemplate.findAndModify(
                                query,
                                update,
                                FindAndModifyOptions.options(),
                                Link.class);
        }

        public void deleteLinkOfUserById(Long idToDelete, String userName) {
                Query query = new Query();
                query.addCriteria(
                                new Criteria().andOperator(
                                                Criteria.where(linkKeys.id()).is(idToDelete),
                                                Criteria.where(linkKeys.ownerUserName()).is(userName)));

                mongoTemplate.findAndRemove(query, Link.class);
        }

        public boolean incrementClickCountIfExists(String shortCode) {
                Query query = new Query();
                query.addCriteria(
                                Criteria.where(linkKeys.hashedKey()).is(shortCode));
                Update update = new Update()
                                .inc(linkKeys.clickCount(), 1);

                return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options(), Link.class) != null;
        }
}
