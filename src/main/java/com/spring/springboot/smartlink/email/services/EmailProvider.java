package com.spring.springboot.smartlink.email.services;

import com.spring.springboot.smartlink.email.config.EmailProviderConfigs;
import com.spring.springboot.smartlink.email.dto.EmailBody;
import com.spring.springboot.smartlink.email.dto.EmailResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class EmailProvider {

    private final WebClient webClient;
    private final EmailProviderConfigs emailProviderConfigs;

    public EmailProvider(WebClient webClient, EmailProviderConfigs emailProviderConfigs) {
        this.webClient = webClient;
        this.emailProviderConfigs = emailProviderConfigs;
    }

    public void send(@NonNull EmailBody emailDto) {
        try {
            webClient.mutate()
                    .baseUrl(emailProviderConfigs.baseUrl())
                    .build()
                    .post()
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", emailProviderConfigs.apiKey())
                    .bodyValue(emailDto)
                    .retrieve()
                    .bodyToMono(EmailResponse.class)
                    .block();

        } catch (Exception e) {
            log.error("Something went wrong during email sending. Exception:  {}", e.getMessage());
        }

    }
}
