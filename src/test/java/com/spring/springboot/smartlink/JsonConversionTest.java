package com.spring.springboot.smartlink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.springboot.smartlink.email.dto.EmailBody;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class JsonConversionTest {

//    @Data
//    @AllArgsConstructor
//    @NoArgsConstructor
//    static class Response{
//        String messageId;
//    }

    @Test
    public void checkJsonStructure() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        EmailBody dto = new EmailBody(
                "sender@gmail.com",
                "Vishal from SmartLink",
                "recipient@gmail.com",
                "recipientName",
                "Brevo Email Api Test",
                "<html><head></head><body><p>testing brevo email api integration</p></body></html>"
        );

        String json = mapper.writeValueAsString(dto);
        log.info(json);


    }
}
