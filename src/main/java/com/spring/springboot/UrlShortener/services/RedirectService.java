package com.spring.springboot.UrlShortener.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.spring.springboot.UrlShortener.dto.RedisDtos.LinkDocForCacheDto;
import com.spring.springboot.UrlShortener.entity.Links;
import com.spring.springboot.UrlShortener.advices.exceptions.ResourceWithHashNotExistsException;
import com.spring.springboot.UrlShortener.dto.RedirectServiceResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedirectService {
    private static final String REDIS_LINK_KEY_PREFIX = "link_with_id_";
    private final MongoTemplate mongoTemplate;
    private final RedisService redisService;
    private final LinkService linkService;

    public RedirectServiceResponseDto getActualUrlIfExists(String hash) {

        Long id = Base62.decode(hash);
//        caching -> get -> found ? return : find in database

        /*
         * minimum information to be stored in redis for url redirection as well as for analysis
         * id -> convertible from hash (for searching in Redis)
         * ownerName -> extracted from actual Link document. (for analysis)
         * actualUrl -> extracted from actual Link document (for redirection)
         */
        try {
            LinkDocForCacheDto linkDocForCacheDto = redisService.get(REDIS_LINK_KEY_PREFIX + id);
            if (linkDocForCacheDto != null) {
                //            actual url
                return RedirectServiceResponseDto.builder()
                        .longUrl(linkDocForCacheDto.getActualUrl())
                        .status(linkDocForCacheDto.getStatus())
                        .build();
            }
        } catch (JsonProcessingException e) {
            log.error("Json parsing error while fetching short link document from the Redis. Exception: {}", e.getMessage());
        }

//        database query -> found ? set in caching (redis) : invalid url

        /* Found** -> insert in redis & push to kafka topic for analysis
         * redis needs
         * conversion from actual document to a *document* that contains minimum affordable information for redirection as well as for analysis
         * id -> convertible from hash (for searching in Redis)
         * status -> extracted from actual Link document. (for redirection)
         * ownerName -> extracted from actual Link document. (for analysis)
         * actualUrl -> extracted from actual Link document (for redirection)
         *
         *
         * analysis needs
         * ip address -> for searching in GeoIp database -> extracted from headers
         * id -> convertible from hash (for searching in Redis)
         * ownerName -> extracted from actual Link document. (for analysis)
         */

        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(id));
        Links linkInDb = mongoTemplate.findOne(query, Links.class);
        if (linkInDb != null) {
//            insert RedisDocument into redis for caching
            LinkDocForCacheDto documentToInsertInRedis = LinkDocForCacheDto.builder()
                    .actualUrl(linkInDb.getActualUrl())
                    .id(linkInDb.getId())
                    .status(linkInDb.getStatus())
                    .ownerUserName(linkInDb.getOwnerUserName())
                    .build();
            try {
                redisService.set(REDIS_LINK_KEY_PREFIX + id, documentToInsertInRedis, 600, TimeUnit.SECONDS);
            } catch (JsonProcessingException e) {
                log.error("Something went wrong while saving the redis document associated with hash: {}. Exception: {}", hash, e.getMessage());
            }

//            analysis -> kafka integration
            return RedirectServiceResponseDto.builder()
                    .longUrl(linkInDb.getActualUrl())
                    .status(linkInDb.getStatus())
                    .build();
        }
//      no short link found
        throw new ResourceWithHashNotExistsException("Invalid url, try with correct url");
    }

    public RedirectServiceResponseDto getUrlStatusIfExists(String hash) {

        return linkService.getLinkStatusIfExists(hash);

    }
}
