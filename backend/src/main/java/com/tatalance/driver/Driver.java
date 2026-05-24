package com.tatalance.driver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "drivers")
public class Driver {
    @Id
    private String id;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{9,14}$", message = "Phone must be E.164 format: + followed by 10-15 digits")
    private String phone;
    private String email;
    private String vehicle;
    @NotNull
    private PayoutType payoutType;
    @NotNull
    private BigDecimal payoutRate;
    private Availability availability = Availability.AVAILABLE;
    private boolean active = true;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getVehicle() { return vehicle; }
    public void setVehicle(String vehicle) { this.vehicle = vehicle; }
    public PayoutType getPayoutType() { return payoutType; }
    public void setPayoutType(PayoutType payoutType) { this.payoutType = payoutType; }
    public BigDecimal getPayoutRate() { return payoutRate; }
    public void setPayoutRate(BigDecimal payoutRate) { this.payoutRate = payoutRate; }
    public Availability getAvailability() { return availability; }
    public void setAvailability(Availability availability) { this.availability = availability; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
