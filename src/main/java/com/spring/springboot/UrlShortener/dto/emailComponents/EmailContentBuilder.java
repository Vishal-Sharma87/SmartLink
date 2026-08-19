package com.spring.springboot.UrlShortener.dto.emailComponents;

import org.springframework.stereotype.Component;

@Component
public class EmailContentBuilder {

    public EmailDto getEmailDtoWithContentUrlShortenedConfirmation(String generatedHash, String longUrl, String email) {

        String shortUrl = "url.shortener/" + generatedHash;
        String content = String.format("""
                Dear User,
                
                Your URL has been successfully shortened and is now available for use.
                
                Short URL: %s
                Original URL: %s
                
                You may start using or sharing the short link immediately.
                
                If you did not initiate this action, please contact our support team.
                
                Regards,
                URLShortener Team
                """, shortUrl, longUrl);

        return EmailDto.builder()
                .subject("Your URL Has Been Shortened")
                .content(content)
                .to(email)
                .build();
    }

    public EmailDto getEmailDtoWithContentSuspiciousUrlWarning(String longUrl, String email) {

        String content = String.format("""
                Dear User,
                
                Our security systems have detected that the following URL you attempted
                to shorten appears to be suspicious or potentially harmful:
                
                URL: %s
                
                For safety reasons, the URL was blocked.
                
                Repeating such attempts may lead to a permanent ban from accessing our services.
                
                If you believe this was flagged incorrectly, kindly reach out to support.
                
                Regards,
                Security Team
                URLShortener
                """, longUrl);

        return EmailDto.builder()
                .subject("Warning: Suspicious URL Detected")
                .content(content)
                .to(email)
                .build();
    }

    public EmailDto getEmailDtoWithContentPermanentBlockNotification(String email) {

        String content = """
                Dear User,
                
                You have exceeded the allowed limit of creating malicious or harmful URLs.
                Due to repeated violations of our safety policies, your account has been
                permanently restricted from using the URLShortener service.
                
                You will no longer be able to create or manage short URLs.
                
                If you believe this action was taken by mistake, you may contact our support team
                for clarification. Please note that such cases undergo strict review.
                
                Regards,
                Compliance & Safety Team
                URLShortener
                """;

        return EmailDto.builder()
                .subject("Account Permanently Blocked")
                .content(content)
                .to(email)
                .build();
    }
    public EmailDto getEmailDtoWithSuccessfulReportContent(String shortUrl, String email) {

        String content = String.format("""
            Dear User,

            We have successfully received your report regarding the following short URL:

            Reported URL: %s

            Our team will now review the details and verify the issue.
            You will be notified once the investigation is complete.

            Thank you for helping us maintain a safe and reliable platform.

            Regards,
            URLShortener Team
            """, shortUrl);

        return EmailDto.builder()
                .subject("Report Received – Investigation in Progress")
                .content(content)
                .to(email)
                .build();
    }

}