package com.spring.springboot.smartlink.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "users")
@Builder
public class User implements UserDetails {

    @Id
    private ObjectId id;

    @NotBlank(message = "Username cannot be blank")
    @Indexed(unique = true)
    private String userName;

    @NotNull(message = "Password cannot be null")

    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @NotNull(message = "Email cannot be null")
    @Email(message = "Invalid email format")
    @Indexed(unique = true)
    private String email;

    private List<String> roles;

    private int maliciousUrlsCreatedCount;

    private Date creationDate;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }

    @Override
    public String getUsername() {
        return userName;
    }
}
