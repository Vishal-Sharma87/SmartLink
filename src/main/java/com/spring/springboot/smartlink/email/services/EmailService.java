package com.spring.springboot.smartlink.email.services;

import com.spring.springboot.smartlink.email.contentbuilder.EmailContentBuilder;
import com.spring.springboot.smartlink.email.dto.EmailBody;
import com.spring.springboot.smartlink.enums.Verdict;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final EmailProvider emailProvider;
    private final EmailContentBuilder emailContentBuilder;

    public EmailService(EmailProvider emailProvider, EmailContentBuilder emailContentBuilder) {
        this.emailProvider = emailProvider;
        this.emailContentBuilder = emailContentBuilder;
    }

    public void sendLinkCreatedEmail(
            String ownerUserName,
            String ownerEmail,
            String shortUrl,
            String longUrl,
            Verdict verdict) {

        EmailBody linkCreated = emailContentBuilder.linkCreatedContent(
                ownerUserName,
                ownerEmail,
                verdict,
                longUrl,
                shortUrl);

        emailProvider.send(linkCreated);
    }

    public void sendLinkCreatedFoundMaliciousEmail(String ownerUserName, String ownerEmail, String shortUrl,
            String longUrl) {
        EmailBody linkFoundMalicious = emailContentBuilder.createdLinkIsMaliciousContent(
                ownerUserName,
                ownerEmail,
                longUrl,
                shortUrl);
        emailProvider.send(linkFoundMalicious);
    }

    public void sendReportAcceptedEmail(String reporterName, String reporterEmail, String linkToReport) {
        EmailBody reportAccepted = emailContentBuilder.reportAcceptedContent(
                reporterName,
                reporterEmail,
                linkToReport);

        emailProvider.send(reportAccepted);
    }

    public void sendWelcomeEmail(
            String userName,
            String userEmail) {

        EmailBody welcomeContent = emailContentBuilder.welcomeContent(userName, userEmail);
        emailProvider.send(welcomeContent);
    }

    public void sendOtpEmail(String email, String otp) {
        EmailBody otpToEmail = emailContentBuilder.otpContent(
                email,
                otp);

        emailProvider.send(otpToEmail);
    }
}
