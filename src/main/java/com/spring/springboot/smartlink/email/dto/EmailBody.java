package com.spring.springboot.smartlink.email.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailBody {
    String htmlContent;
    Sender sender;
    String subject;
    Recipient[] to;

    public EmailBody(
            String senderEmail,
            String senderName,
            String recipientEmail,
            String recipientName,
            String subject,
            String htmlContent
            ){

        this.sender = new Sender(senderEmail, senderName);
        this.htmlContent = htmlContent;
        this.subject = subject;
        this.to = new Recipient[]{new Recipient(recipientEmail, recipientName)};
    }


    @Data
    @NoArgsConstructor
    private static class Recipient{
        String email;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String name;

        public Recipient(String email, String name){
            this.email = email;
            this.name = name;
        }

    }


    @Data
    @NoArgsConstructor
    private static class Sender{
        String email;
        String name;

        public Sender(String senderEmail, String senderName){
            this.name = senderName;
            this.email = senderEmail;
        }

    }
}
