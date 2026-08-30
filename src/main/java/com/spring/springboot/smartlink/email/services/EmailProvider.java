package com.spring.springboot.smartlink.email.services;

import com.spring.springboot.smartlink.email.dto.EmailBody;
import com.spring.springboot.smartlink.email.dto.EmailResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class EmailProvider {

    private final WebClient webClient;
    private final String baseUrl;
    private final String apiKey;

    public EmailProvider(WebClient webClient,
                         @Value("${email.provider.base-url}") String baseUrl,
                         @Value("${email.provider.api-key}") String apiKey){

        this.webClient = webClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }


    public void send(@NonNull EmailBody emailDto) {
        try {
            webClient.mutate()
                    .baseUrl(baseUrl)
                    .build()
                    .post()
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", apiKey)
                    .bodyValue(emailDto)
                    .retrieve()
                    .bodyToMono(EmailResponse.class)
                    .block();

        }catch (Exception e){
            log.error("Something went wrong during email sending. Exception:  {}",e.getMessage());
        }

    }
}
