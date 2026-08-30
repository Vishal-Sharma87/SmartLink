package com.spring.springboot.smartlink.email.contentbuilder;

import com.spring.springboot.smartlink.email.dto.EmailBody;
import com.spring.springboot.smartlink.enums.Verdict;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailContentBuilder {



    private static final String HTML_TEMPLATE_WITH_ONE_PLACEHOLDER = "<html><head></head><body>%s</body></html";
    private static final String P_TAG_TEMPLATE = "<p>%s</p>";
    private static final String OTP_MESSAGE_WITH_ONE_PLACEHOLDER = "Your OTP is: %s";
    private static final String OTP_CONTENT_TEMPLATE =
            HTML_TEMPLATE_WITH_ONE_PLACEHOLDER.formatted(P_TAG_TEMPLATE.formatted(OTP_MESSAGE_WITH_ONE_PLACEHOLDER));

    private static final String WELCOME_BODY =
            "<p>Hi %s,</p>"
                    + "<p>Welcome to SmartLink! We're glad to have you on board.</p>"
                    + "<p>You can now create short links, track their performance, and keep your audience safe with our built-in malicious link detection.</p>"
                    + "<p>— The SmartLink Team</p>";
    private static final String WELCOME_TEMPLATE =
            HTML_TEMPLATE_WITH_ONE_PLACEHOLDER.formatted(WELCOME_BODY);

    private static final String REPORT_ACCEPTED_BODY =
            "<p>Hi %s,</p>"
                    + "<p>Thank you for reporting a suspicious link. Our team has reviewed your report and confirmed it.</p>"
                    + "<p>Reported link: <a href=\"%s\">%s</a></p>"
                    + "<p>We appreciate you helping keep SmartLink safe for everyone.</p>"
                    + "<p>— The SmartLink Team</p>";
    private static final String REPORT_ACCEPTED_TEMPLATE =
            HTML_TEMPLATE_WITH_ONE_PLACEHOLDER.formatted(REPORT_ACCEPTED_BODY);

    private static final String LINK_MALICIOUS_BODY =
            "<p>Hi %s,</p>"
                    + "<p>We've detected that a link you created has been flagged as <strong>malicious</strong> and has been disabled.</p>"
                    + "<p>Original URL: %s</p>"
                    + "<p>Short URL: <a href=\"%s\">%s</a></p>"
                    + "<p>If you believe this is a mistake, please contact our support team.</p>"
                    + "<p>— The SmartLink Team</p>";
    private static final String LINK_MALICIOUS_TEMPLATE =
            HTML_TEMPLATE_WITH_ONE_PLACEHOLDER.formatted(LINK_MALICIOUS_BODY);

    private static final String LINK_CREATED_BODY =
            "<p>Hi %s,</p>"
                    + "<p>Your short link has been created successfully.</p>"
                    + "<p>Original URL: %s</p>"
                    + "<p>Short URL: <a href=\"%s\">%s</a></p>"
                    + "<p>Scan status: <strong>%s</strong></p>"
                    + "<p>— The SmartLink Team</p>";
    private static final String LINK_CREATED_TEMPLATE =
            HTML_TEMPLATE_WITH_ONE_PLACEHOLDER.formatted(LINK_CREATED_BODY);

    private final String senderEmail;
    private final String senderName;
    private final String otpSubject;
    private final String welcomeSubject;
    private final String reportAcceptedSubject;
    private final String linkMaliciousSubject;
    private final String linkCreatedSubject;

    public EmailContentBuilder(
            @Value("${email.sender.email}") String senderEmail,
            @Value("${email.sender.name}") String senderName,
            @Value("${email.subject.otp}") String otpSubject,
            @Value("${email.subject.welcome}") String welcomeSubject,
            @Value("${email.subject.report-accepted}") String reportAcceptedSubject,
            @Value("${email.subject.link-malicious}") String linkMaliciousSubject,
            @Value("${email.subject.link-created}") String linkCreatedSubject
    ) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.otpSubject = otpSubject;
        this.welcomeSubject = welcomeSubject;
        this.reportAcceptedSubject = reportAcceptedSubject;
        this.linkMaliciousSubject = linkMaliciousSubject;
        this.linkCreatedSubject = linkCreatedSubject;
    }

    public EmailBody reportAcceptedContent(String reporterName, String reporterEmail, String linkToReport) {
        return new EmailBody(
                senderEmail,
                senderName,
                reporterEmail,
                reporterName,
                reportAcceptedSubject,
                REPORT_ACCEPTED_TEMPLATE.formatted(reporterName, linkToReport, linkToReport));
    }

    public EmailBody welcomeContent(String userName, String userEmail) {
        return new EmailBody(
                senderEmail,
                senderName,
                userEmail,
                userName,
                welcomeSubject,
                WELCOME_TEMPLATE.formatted(userName));
    }

    public EmailBody createdLinkIsMaliciousContent(
            String userName,
            String userEmail,
            String longUrl,
            String shortUrl) {
        return new EmailBody(
                senderEmail,
                senderName,
                userEmail,
                userName,
                linkMaliciousSubject,
                LINK_MALICIOUS_TEMPLATE.formatted(userName, longUrl, shortUrl, shortUrl));
    }

    public EmailBody linkCreatedContent(
            String ownerUserName,
            String userEmail,
            Verdict scannedVerdict,
            String longUrl,
            String shortUrl) {
        return new EmailBody(
                senderEmail,
                senderName,
                userEmail,
                ownerUserName,
                linkCreatedSubject,
                LINK_CREATED_TEMPLATE.formatted(ownerUserName, longUrl, shortUrl, shortUrl, scannedVerdict.name()));
    }

    public EmailBody otpContent(String toEmail, String otp) {
        return new EmailBody(
                senderEmail,
                senderName,
                toEmail,
                null,
                otpSubject,
                OTP_CONTENT_TEMPLATE.formatted(otp));
    }
}