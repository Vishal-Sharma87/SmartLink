package com.spring.springboot.smartlink.email.services;

import com.spring.springboot.smartlink.email.dto.EmailBody;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final EmailProvider emailProvider;

    public EmailService(EmailProvider emailProvider){
        this.emailProvider = emailProvider;
    }

    public void sendEmail(EmailBody emailDto) {
        emailProvider.send(emailDto);
    }
}
