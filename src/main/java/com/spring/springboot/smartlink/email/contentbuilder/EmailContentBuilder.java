package com.spring.springboot.smartlink.email.contentbuilder;

import com.spring.springboot.smartlink.email.config.EmailSenderConfigs;
import com.spring.springboot.smartlink.email.config.EmailSubject;
import com.spring.springboot.smartlink.email.dto.EmailBody;
import com.spring.springboot.smartlink.enums.Verdict;
import org.springframework.stereotype.Component;

@Component
public class EmailContentBuilder {

        private static final String HTML_TEMPLATE_WITH_ONE_PLACEHOLDER = "<html><head></head><body>%s</body></html";
        private static final String P_TAG_TEMPLATE = "<p>%s</p>";
        private static final String OTP_MESSAGE_WITH_ONE_PLACEHOLDER = "Your OTP is: %s";
        private static final String OTP_CONTENT_TEMPLATE = HTML_TEMPLATE_WITH_ONE_PLACEHOLDER
                        .formatted(P_TAG_TEMPLATE.formatted(OTP_MESSAGE_WITH_ONE_PLACEHOLDER));

        private static final String WELCOME_BODY = "<p>Hi %s,</p>"
                        + "<p>Welcome to SmartLink! We're glad to have you on board.</p>"
                        + "<p>You can now create short links, track their performance, and keep your audience safe with our built-in malicious link detection.</p>"
                        + "<p>— The SmartLink Team</p>";

        private static final String WELCOME_TEMPLATE = HTML_TEMPLATE_WITH_ONE_PLACEHOLDER.formatted(WELCOME_BODY);

        private static final String REPORT_ACCEPTED_BODY = "<p>Hi %s,</p>"
                        + "<p>Thank you for reporting a suspicious link. Our team has reviewed your report and confirmed it.</p>"
                        + "<p>Reported link: <a href=\"%s\">%s</a></p>"
                        + "<p>We appreciate you helping keep SmartLink safe for everyone.</p>"
                        + "<p>— The SmartLink Team</p>";

        private static final String REPORT_ACCEPTED_TEMPLATE = HTML_TEMPLATE_WITH_ONE_PLACEHOLDER
                        .formatted(REPORT_ACCEPTED_BODY);

        private static final String LINK_MALICIOUS_BODY = "<p>Hi %s,</p>"
                        + "<p>We've detected that a link you created has been flagged as <strong>malicious</strong> and has been disabled.</p>"
                        + "<p>Original URL: %s</p>"
                        + "<p>Short URL: <a href=\"%s\">%s</a></p>"
                        + "<p>If you believe this is a mistake, please contact our support team.</p>"
                        + "<p>— The SmartLink Team</p>";

        private static final String LINK_MALICIOUS_TEMPLATE = HTML_TEMPLATE_WITH_ONE_PLACEHOLDER
                        .formatted(LINK_MALICIOUS_BODY);

        private static final String LINK_CREATED_BODY = "<p>Hi %s,</p>"
                        + "<p>Your short link has been created successfully.</p>"
                        + "<p>Original URL: %s</p>"
                        + "<p>Short URL: <a href=\"%s\">%s</a></p>"
                        + "<p>Scan status: <strong>%s</strong></p>"
                        + "<p>— The SmartLink Team</p>";

        private static final String LINK_CREATED_TEMPLATE = HTML_TEMPLATE_WITH_ONE_PLACEHOLDER
                        .formatted(LINK_CREATED_BODY);

        private final EmailSenderConfigs emailSenderConfigs;
        private final EmailSubject emailSubject;

        public EmailContentBuilder(EmailSenderConfigs emailSenderConfigs, EmailSubject emailSubject) {
                this.emailSenderConfigs = emailSenderConfigs;
                this.emailSubject = emailSubject;
        }

        public EmailBody reportAcceptedContent(String reporterName, String reporterEmail, String linkToReport) {
                return new EmailBody(
                                emailSenderConfigs.email(),
                                emailSenderConfigs.name(),
                                reporterEmail,
                                reporterName,
                                emailSubject.reportAccepted(),
                                REPORT_ACCEPTED_TEMPLATE.formatted(reporterName, linkToReport, linkToReport));
        }

        public EmailBody welcomeContent(String userName, String userEmail) {
                return new EmailBody(
                                emailSenderConfigs.email(),
                                emailSenderConfigs.name(),
                                userEmail,
                                userName,
                                emailSubject.welcome(),
                                WELCOME_TEMPLATE.formatted(userName));
        }

        public EmailBody createdLinkIsMaliciousContent(
                        String userName,
                        String userEmail,
                        String longUrl,
                        String shortUrl) {
                return new EmailBody(
                                emailSenderConfigs.email(),
                                emailSenderConfigs.name(),
                                userEmail,
                                userName,
                                emailSubject.linkMalicious(),
                                LINK_MALICIOUS_TEMPLATE.formatted(userName, longUrl, shortUrl, shortUrl));
        }

        public EmailBody linkCreatedContent(
                        String ownerUserName,
                        String userEmail,
                        Verdict scannedVerdict,
                        String longUrl,
                        String shortUrl) {
                return new EmailBody(
                                emailSenderConfigs.email(),
                                emailSenderConfigs.name(),
                                userEmail,
                                ownerUserName,
                                emailSubject.linkCreated(),
                                LINK_CREATED_TEMPLATE.formatted(ownerUserName, longUrl, shortUrl, shortUrl,
                                                scannedVerdict.name()));
        }

        public EmailBody otpContent(String toEmail, String otp) {
                return new EmailBody(
                                emailSenderConfigs.email(),
                                emailSenderConfigs.name(),
                                toEmail,
                                null,
                                emailSubject.otp(),
                                OTP_CONTENT_TEMPLATE.formatted(otp));
        }
}