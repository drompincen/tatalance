package com.tatalance.profile;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Profile for a business owner account.
 * An AppUser (account) can have multiple profiles (e.g. DRIVER, ENGINEER).
 * Jobs are linked to a specific profile; clients are shared at account level.
 */
@Document(collection = "profiles")
public class Profile {
    @Id
    private String id;

    @Indexed
    private String userId;

    private ProfileType type;

    private String name; // optional display name for the profile/business

    /** Optional override; decimal fraction (0.08 = 8%). Null = use account default. */
    private BigDecimal taxRate;

    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public ProfileType getType() { return type; }
    public void setType(ProfileType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
