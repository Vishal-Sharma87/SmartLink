package com.spring.springboot.smartlink.services;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.spring.springboot.smartlink.dto.emailComponents.EmailContentBuilder;
import com.spring.springboot.smartlink.dto.emailComponents.EmailDto;
import com.spring.springboot.smartlink.entity.User;
import com.spring.springboot.smartlink.advices.exceptions.SendgridEmailFailedException;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.model.LinkCreationDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailContentBuilder emailContentBuilder;
    private final UserService userService;
    @Value("${sendgrid.api_key}")
    private String sendgridApiKey;
    @Value("${sendgrid.verified.senders.email}")
    private String sendgridVerifiedSendersEmail;

    private String buildMail(String to, String sub, String body) throws IOException {
        Email from = new Email(sendgridVerifiedSendersEmail);

        Email recipientsEmail = new Email(to);

        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, sub, recipientsEmail, content);
        return mail.build();
    }

    private void getSendGridResponse(EmailDto dto) throws IOException {

        String mail = buildMail(dto.getTo(), dto.getSubject(), dto.getContent());

        SendGrid sg = new SendGrid(sendgridApiKey);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail);
        sg.api(request);
    }

    public void sendEmail(EmailDto dto) throws IOException {
//        some functionality according to the response from sendgrid
        getSendGridResponse(dto);
        log.debug("Email delivery completed successfully");
    }

    public void sendEmail(Verdict verdict, User userInDb, LinkCreationDto linkCreationDto) {

        EmailDto dto;
        int malCnt = userInDb.getMaliciousUrlsCreatedCount();

        switch (verdict) {
            case MALICIOUS, SUSPICIOUS -> {
                if (malCnt + 1 >= 5)
                    dto = emailContentBuilder.getEmailDtoWithContentPermanentBlockNotification(userInDb.getEmail());
                else
                    dto = emailContentBuilder.getEmailDtoWithContentSuspiciousUrlWarning(linkCreationDto.getLongUrl(), userInDb.getEmail());

                userInDb.setMaliciousUrlsCreatedCount(malCnt + 1);
                userService.save(userInDb);

            }
            default ->
                    dto = emailContentBuilder.getEmailDtoWithContentUrlShortenedConfirmation(linkCreationDto.getGeneratedHash(), linkCreationDto.getLongUrl(), userInDb.getEmail());
        }
        try {
            sendEmail(dto);
        } catch (IOException e) {
            log.error("Failed to send link status email for short code {}", linkCreationDto.getGeneratedHash(), e);
            throw new SendgridEmailFailedException("Process to send status of newly created short Link has failed, Exception: " + e.getMessage());
        }
    }
}
