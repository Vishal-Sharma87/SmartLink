package com.spring.springboot.smartlink.email.contentbuilder;

import com.spring.springboot.smartlink.email.config.EmailSenderConfigs;
import com.spring.springboot.smartlink.email.config.EmailSubject;
import com.spring.springboot.smartlink.email.dto.EmailBody;
import com.spring.springboot.smartlink.enums.Verdict;
import org.springframework.stereotype.Component;

@Component
public class EmailContentBuilder {

        private static final String HTML_TEMPLATE = "<html><head></head><body>%s</body></html>";
        private static final String P_TAG_TEMPLATE = "<p>%s</p>";

        private static final String OTP_MESSAGE = "Your OTP is: %s";
        private static final String OTP_TEMPLATE = HTML_TEMPLATE.formatted(
                P_TAG_TEMPLATE.formatted(OTP_MESSAGE)
        );

        private static final String WELCOME_BODY =
                "<p>Hi %s,</p>"
                        + "<p>Welcome to SmartLink! We're glad to have you on board.</p>"
                        + "<p>You can now create short links, track their performance, and keep your audience safe with our built-in malicious link detection.</p>"
                        + "<p>— The SmartLink Team</p>";

        private static final String WELCOME_TEMPLATE = HTML_TEMPLATE.formatted(WELCOME_BODY);

        private static final String REPORT_ACCEPTED_BODY =
                "<p>Hi %s,</p>"
                        + "<p>Thank you for reporting a suspicious link. Our team has reviewed your report and confirmed it.</p>"
                        + "<p>Reported link: <a href=\"%s\">%s</a></p>"
                        + "<p>We appreciate you helping keep SmartLink safe for everyone.</p>"
                        + "<p>— The SmartLink Team</p>";

        private static final String REPORT_ACCEPTED_TEMPLATE =
                HTML_TEMPLATE.formatted(REPORT_ACCEPTED_BODY);

        private static final String LINK_MALICIOUS_BODY =
                "<p>Hi %s,</p>"
                        + "<p>We've detected that a link you created has been flagged as <strong>malicious</strong> and has been disabled.</p>"
                        + "<p>Original URL: %s</p>"
                        + "<p>Short URL: <a href=\"%s\">%s</a></p>"
                        + "<p>If you believe this is a mistake, please contact our support team.</p>"
                        + "<p>— The SmartLink Team</p>";

        private static final String LINK_MALICIOUS_TEMPLATE =
                HTML_TEMPLATE.formatted(LINK_MALICIOUS_BODY);

        private static final String LINK_CREATED_BODY =
                "<p>Hi %s,</p>"
                        + "<p>Your short link has been created successfully.</p>"
                        + "<p>Original URL: %s</p>"
                        + "<p>Short URL: <a href=\"%s\">%s</a></p>"
                        + "<p>Scan status: <strong>%s</strong></p>"
                        + "<p>— The SmartLink Team</p>";

        private static final String LINK_CREATED_TEMPLATE =
                HTML_TEMPLATE.formatted(LINK_CREATED_BODY);

        private final EmailSenderConfigs emailSenderConfigs;
        private final EmailSubject emailSubject;

        public EmailContentBuilder(
                EmailSenderConfigs emailSenderConfigs,
                EmailSubject emailSubject
        ) {
                this.emailSenderConfigs = emailSenderConfigs;
                this.emailSubject = emailSubject;
        }

        public EmailBody createdLinkIsMaliciousContent(
                String ownerEmail,
                String originalUrl,
                String shortUrl
        ) {
                return new EmailBody(
                        emailSenderConfigs.email(),
                        emailSenderConfigs.name(),
                        ownerEmail,
                        null,
                        emailSubject.linkMalicious(),
                        LINK_MALICIOUS_TEMPLATE.formatted(
                                ownerEmail,
                                originalUrl,
                                shortUrl,
                                shortUrl
                        )
                );
        }

        public EmailBody reportAcceptedContent(
                String reporterName,
                String reporterEmail,
                String linkToReport
        ) {
                return new EmailBody(
                        emailSenderConfigs.email(),
                        emailSenderConfigs.name(),
                        reporterEmail,
                        reporterName,
                        emailSubject.reportAccepted(),
                        REPORT_ACCEPTED_TEMPLATE.formatted(
                                reporterName,
                                linkToReport,
                                linkToReport
                        )
                );
        }

        public EmailBody welcomeContent(
                String firstName,
                String userEmail
        ) {
                return new EmailBody(
                        emailSenderConfigs.email(),
                        emailSenderConfigs.name(),
                        userEmail,
                        firstName,
                        emailSubject.welcome(),
                        WELCOME_TEMPLATE.formatted(firstName)
                );
        }

        public EmailBody linkCreatedContent(
                String userEmail,
                String originalUrl,
                String shortUrl,
                String urlToDisplay,
                Verdict scannedVerdict
        ) {
                return new EmailBody(
                        emailSenderConfigs.email(),
                        emailSenderConfigs.name(),
                        userEmail,
                        null,
                        emailSubject.linkCreated(),
                        LINK_CREATED_TEMPLATE.formatted(
                                userEmail,
                                originalUrl,
                                shortUrl,
                                urlToDisplay,
                                scannedVerdict.name()
                        )
                );
        }

        public EmailBody otpContent(
                String userEmail,
                String otp
        ) {
                return new EmailBody(
                        emailSenderConfigs.email(),
                        emailSenderConfigs.name(),
                        userEmail,
                        null,
                        emailSubject.otp(),
                        OTP_TEMPLATE.formatted(otp)
                );
        }
}