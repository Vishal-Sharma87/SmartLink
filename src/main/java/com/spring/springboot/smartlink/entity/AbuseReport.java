package com.spring.springboot.smartlink.entity;


import com.spring.springboot.smartlink.enums.ReportCause;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Document(collection = "link_reports")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbuseReport {

    @Id
    private ObjectId id;

    @NotNull
    @NotBlank
    private String hashedKeyOfLink;

    @NotBlank
    @NotNull
    private String reporterName;

    @Email
    private String reporterEmail;

    private List<ReportCause> cause;

    private String description;

    private LocalDateTime createdAt;

    private String reportStatus;
}
