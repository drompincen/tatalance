package com.tatalance.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "users")
public class AppUser {
    @Id
    private String id;
    @Indexed(unique = true)
    private String username;
    private String password;
    private String role;
    private Instant createdAt;
    private String googleId;
    private String securityQuestion;
    private String securityAnswer;
    private BusinessMode businessMode = BusinessMode.CHAUFFEUR;
    private BigDecimal defaultHourlyRate = new BigDecimal("20.00");

    // Business owner and type concepts (for multi-profile support)
    private boolean businessOwner = true;
    private String businessOwnerType; // optional high-level type for the account

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public String getSecurityQuestion() { return securityQuestion; }
    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }
    public String getSecurityAnswer() { return securityAnswer; }
    public void setSecurityAnswer(String securityAnswer) { this.securityAnswer = securityAnswer; }
    public BusinessMode getBusinessMode() { return businessMode; }
    public void setBusinessMode(BusinessMode businessMode) { this.businessMode = businessMode; }
    public BigDecimal getDefaultHourlyRate() { return defaultHourlyRate; }
    public void setDefaultHourlyRate(BigDecimal defaultHourlyRate) { this.defaultHourlyRate = defaultHourlyRate; }

    public boolean isBusinessOwner() { return businessOwner; }
    public void setBusinessOwner(boolean businessOwner) { this.businessOwner = businessOwner; }
    public String getBusinessOwnerType() { return businessOwnerType; }
    public void setBusinessOwnerType(String businessOwnerType) { this.businessOwnerType = businessOwnerType; }
}
