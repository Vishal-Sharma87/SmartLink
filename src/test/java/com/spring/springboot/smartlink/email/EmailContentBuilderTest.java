package com.spring.springboot.smartlink.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.springboot.smartlink.email.config.EmailSenderConfigs;
import com.spring.springboot.smartlink.email.config.EmailSubject;
import com.spring.springboot.smartlink.email.contentbuilder.EmailContentBuilder;
import com.spring.springboot.smartlink.email.dto.EmailBody;
import com.spring.springboot.smartlink.enums.Verdict;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailContentBuilderTest {

    private static final String SENDER_EMAIL = "noreply@smartlink.com";
    private static final String SENDER_NAME = "SmartLink";

    private static final String USER_EMAIL = "user@example.com";
    private static final String REPORTER_EMAIL = "reporter@example.com";
    private static final String REPORTER_NAME = "Rahul";

    private static final String ORIGINAL_URL = "https://example.com";
    private static final String SHORT_URL = "https://smart.link/abc123";
    private static final String OTP = "123456";

    @Mock
    private EmailSenderConfigs emailSenderConfigs;

    @Mock
    private EmailSubject emailSubject;

    private EmailContentBuilder emailContentBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(emailSenderConfigs.email()).thenReturn(SENDER_EMAIL);
        when(emailSenderConfigs.name()).thenReturn(SENDER_NAME);

        emailContentBuilder = new EmailContentBuilder(
                emailSenderConfigs,
                emailSubject
        );
    }

    @Test
    void shouldBuildOtpContent() {
        when(emailSubject.otp()).thenReturn("otp");

        EmailBody emailBody = emailContentBuilder.otpContent(
                USER_EMAIL,
                OTP
        );

        JsonNode json = toJson(emailBody);

        assertCommonFields(
                json,
                USER_EMAIL,
                null,
                "otp"
        );

        assertContainsInOrder(
                json.get("htmlContent").asText(),
                OTP
        );
    }

    @Test
    void shouldBuildWelcomeContent() {
        when(emailSubject.welcome()).thenReturn("welcome");

        EmailBody emailBody = emailContentBuilder.welcomeContent(
                "Vishal",
                USER_EMAIL
        );

        JsonNode json = toJson(emailBody);

        assertCommonFields(
                json,
                USER_EMAIL,
                "Vishal",
                "welcome"
        );

        assertContainsInOrder(
                json.get("htmlContent").asText(),
                "Vishal"
        );
    }

    @Test
    void shouldBuildReportAcceptedContent() {
        when(emailSubject.reportAccepted()).thenReturn("report-accepted");

        EmailBody emailBody = emailContentBuilder.reportAcceptedContent(
                REPORTER_NAME,
                REPORTER_EMAIL,
                ORIGINAL_URL
        );

        JsonNode json = toJson(emailBody);

        assertCommonFields(
                json,
                REPORTER_EMAIL,
                REPORTER_NAME,
                "report-accepted"
        );

        assertContainsInOrder(
                json.get("htmlContent").asText(),
                REPORTER_NAME,
                ORIGINAL_URL,
                ORIGINAL_URL
        );
    }

    @Test
    void shouldBuildMaliciousLinkContent() {
        when(emailSubject.linkMalicious()).thenReturn("link-malicious");

        EmailBody emailBody = emailContentBuilder.createdLinkIsMaliciousContent(
                USER_EMAIL,
                ORIGINAL_URL,
                SHORT_URL
        );

        JsonNode json = toJson(emailBody);

        assertCommonFields(
                json,
                USER_EMAIL,
                null,
                "link-malicious"
        );

        assertContainsInOrder(
                json.get("htmlContent").asText(),
                USER_EMAIL,
                ORIGINAL_URL,
                SHORT_URL,
                SHORT_URL
        );
    }

    @Test
    void shouldBuildLinkCreatedContent() {
        when(emailSubject.linkCreated()).thenReturn("link-created");

        EmailBody emailBody = emailContentBuilder.linkCreatedContent(
                USER_EMAIL,
                ORIGINAL_URL,
                SHORT_URL,
                SHORT_URL+ "Displayed" ,
                Verdict.SAFE
        );

        JsonNode json = toJson(emailBody);

        assertCommonFields(
                json,
                USER_EMAIL,
                null,
                "link-created"
        );

        assertContainsInOrder(
                json.get("htmlContent").asText(),
                USER_EMAIL,
                ORIGINAL_URL,
                SHORT_URL,
                SHORT_URL,
                Verdict.SAFE.name()
        );
    }

    @ParameterizedTest
    @MethodSource("verdicts")
    void shouldPlaceVerdictCorrectly(Verdict verdict) {
        when(emailSubject.linkCreated()).thenReturn("link-created");

        EmailBody emailBody = emailContentBuilder.linkCreatedContent(
                USER_EMAIL,
                ORIGINAL_URL,
                SHORT_URL,
                SHORT_URL+ "Displayed" ,
                verdict
        );

        String htmlContent = toJson(emailBody)
                .get("htmlContent")
                .asText();

        assertContainsInOrder(
                htmlContent,
                USER_EMAIL,
                ORIGINAL_URL,
                SHORT_URL,
                SHORT_URL,
                verdict.name()
        );
    }

    static Stream<Arguments> verdicts() {
        return Stream.of(
                Arguments.of(Verdict.SAFE),
                Arguments.of(Verdict.SUSPICIOUS),
                Arguments.of(Verdict.MALICIOUS),
                Arguments.of(Verdict.PENDING_REVERIFICATION)
        );
    }

    private JsonNode toJson(EmailBody emailBody) {
        return objectMapper.valueToTree(emailBody);
    }

    private void assertCommonFields(
            JsonNode json,
            String expectedRecipientEmail,
            String expectedRecipientName,
            String expectedSubject
    ) {
        assertNotNull(json);

        assertEquals(
                SENDER_EMAIL,
                json.get("sender").get("email").asText()
        );

        assertEquals(
                SENDER_NAME,
                json.get("sender").get("name").asText()
        );

        assertEquals(
                expectedSubject,
                json.get("subject").asText()
        );

        JsonNode recipient = json.get("to").get(0);

        assertEquals(
                expectedRecipientEmail,
                recipient.get("email").asText()
        );

        if (expectedRecipientName == null) {
            assertNull(recipient.get("name"));
        } else {
            assertEquals(
                    expectedRecipientName,
                    recipient.get("name").asText()
            );
        }
    }

    private void assertContainsInOrder(
            String content,
            String... expectedValues
    ) {
        int previousIndex = -1;

        for (String expectedValue : expectedValues) {
            int currentIndex = content.indexOf(
                    expectedValue,
                    previousIndex + 1
            );

            assertTrue(
                    currentIndex >= 0,
                    "Expected value not found: " + expectedValue
            );

            assertTrue(
                    currentIndex > previousIndex,
                    "Expected value '" + expectedValue
                            + "' to appear after the previous value"
            );

            previousIndex = currentIndex;
        }
    }
}