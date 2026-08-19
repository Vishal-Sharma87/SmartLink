package com.spring.springboot.UrlShortener.services;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.spring.springboot.UrlShortener.dto.emailComponents.EmailContentBuilder;
import com.spring.springboot.UrlShortener.dto.emailComponents.EmailDto;
import com.spring.springboot.UrlShortener.entity.UrlUser;
import com.spring.springboot.UrlShortener.enums.FinalVerdict;
import com.spring.springboot.UrlShortener.advices.exceptions.SendgridEmailFailedException;
import com.spring.springboot.UrlShortener.model.LinkCreationDto;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
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
    }

    public void sendEmail(FinalVerdict.Verdict verdict, UrlUser userInDb, LinkCreationDto linkCreationDto) {

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
            throw new SendgridEmailFailedException("Process to send status of newly created short Link has failed, Exception: " + e.getMessage());
        }
    }
}
