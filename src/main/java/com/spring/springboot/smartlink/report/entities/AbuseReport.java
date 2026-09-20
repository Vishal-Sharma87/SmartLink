package com.spring.springboot.smartlink.report.entities;

import com.spring.springboot.smartlink.enums.ReportCause;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "link_reports")
@AllArgsConstructor
@Builder
public class AbuseReport {

    @Id
    private final ObjectId id;

    @NotNull
    @NotBlank
    private final String shortCode;

    @NotBlank
    @NotNull
    private final String reporterName;

    @Email
    private final String reporterEmail;

    private final List<ReportCause> cause;

    private final String description;

    private final LocalDateTime createdAt;

    private final String reportStatus;
}
