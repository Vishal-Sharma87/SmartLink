package com.spring.springboot.smartlink.dto.emailComponents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDto {

    private String to;
    private String subject;
    private String content;
}
