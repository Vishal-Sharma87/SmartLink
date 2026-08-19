package com.spring.springboot.UrlShortener.entity;

import com.spring.springboot.UrlShortener.enums.Verdict;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


import java.util.Date;
import java.util.List;

@Document(collection = "links")
@Data
@Builder
public class Links {

    //    base10 value of counter variable stored in redis
    @Id
    private Long id;


    private String actualUrl;

    //     base62 conversion of counter variable stored in redis
    private String hashedKey;

    //    owner of link, for analysis purposes
    @Indexed
    private String ownerUserName;


    //    date of creation of a url
    private Date linkCreationTime;

    //   current status of the url either safe, malicious or suspicious
    private Verdict status;


    //    url reports
    private List<AbuseReport> abuseReports;

    //    if someone report the link as unsafe ,then we will increment the counter
    //    if it hits a predefined threshold then will send a mail to owner to take action on it
    private int reportCount;

    //    for analysis purposes
    private Date firstReportedTime;

    private Date lastReportedTime;

    //    how many times the link is clicked
    private Integer clickCount;

    public void incrementClickCount() {
        clickCount++;
    }
}
