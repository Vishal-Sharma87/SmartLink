package com.spring.springboot.smartlink.email.services;

import com.spring.springboot.smartlink.configurations.ApplicationConfigs;
import com.spring.springboot.smartlink.email.contentbuilder.EmailContentBuilder;
import com.spring.springboot.smartlink.email.dto.EmailBody;
import com.spring.springboot.smartlink.enums.Verdict;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final EmailProvider emailProvider;
    private final EmailContentBuilder emailContentBuilder;
    private final ApplicationConfigs applicationConfigs;

    public EmailService(EmailProvider emailProvider, EmailContentBuilder emailContentBuilder, ApplicationConfigs applicationConfigs) {
        this.emailProvider = emailProvider;
        this.emailContentBuilder = emailContentBuilder;
        this.applicationConfigs = applicationConfigs;
    }

    public void sendLinkCreatedEmail(
            String ownerEmail,
            String originalUrl,
            String shortCode,
            Verdict verdict) {

        String urlToDisplay = applicationConfigs.domain() + "/" + shortCode;
        String shortUrl = applicationConfigs.shortUrlPrefix() + "/" + shortCode;

        EmailBody linkCreated = emailContentBuilder.linkCreatedContent(
                ownerEmail,
                originalUrl,
                shortUrl,
                urlToDisplay,
                verdict);

        emailProvider.send(linkCreated);
    }

    public void sendLinkCreatedFoundMaliciousEmail(
            String ownerEmail,
            String shortUrl,
            String originalUrl) {
        
        EmailBody linkFoundMalicious = emailContentBuilder.createdLinkIsMaliciousContent(
                ownerEmail,
                originalUrl,
                shortUrl);
        emailProvider.send(linkFoundMalicious);
    }

    public void sendReportAcceptedEmail(
            String reporterName,
            String reporterEmail,
            String linkToReport) {

        EmailBody reportAccepted = emailContentBuilder.reportAcceptedContent(
                reporterName,
                reporterEmail,
                linkToReport);

        emailProvider.send(reportAccepted);
    }

    public void sendWelcomeEmail(
            String firstName,
            String userEmail) {

        EmailBody welcomeContent = emailContentBuilder.welcomeContent(firstName, userEmail);
        emailProvider.send(welcomeContent);
    }

    public void sendOtpEmail(
            String email,
            String otp) {

        EmailBody otpToEmail = emailContentBuilder.otpContent(
                email,
                otp);

        emailProvider.send(otpToEmail);
    }
}
