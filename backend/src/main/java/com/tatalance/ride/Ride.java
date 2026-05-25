package com.tatalance.ride;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "rides")
public class Ride {
    @Id
    private String id;
    @NotBlank
    private String clientId;
    private String clientName;
    @NotNull
    private Instant pickupDateTime;
    @NotBlank
    private String pickupLocation;
    @NotBlank
    private String dropoffLocation;
    private BigDecimal basePrice;
    private String notes;
    private RideStatus status = RideStatus.SCHEDULED;
    private String assignedDriverId;
    private Instant createdAt;

    // Completion details (set by POST /api/rides/{id}/start and /complete — M4, #34)
    private Instant actualStart;
    private Instant actualEnd;
    private BigDecimal tolls;
    private BigDecimal parking;
    private BigDecimal additionalCharges;
    private BigDecimal billableAmount;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public Instant getPickupDateTime() { return pickupDateTime; }
    public void setPickupDateTime(Instant pickupDateTime) { this.pickupDateTime = pickupDateTime; }
    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
    public String getDropoffLocation() { return dropoffLocation; }
    public void setDropoffLocation(String dropoffLocation) { this.dropoffLocation = dropoffLocation; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public RideStatus getStatus() { return status; }
    public void setStatus(RideStatus status) { this.status = status; }
    public String getAssignedDriverId() { return assignedDriverId; }
    public void setAssignedDriverId(String assignedDriverId) { this.assignedDriverId = assignedDriverId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getActualStart() { return actualStart; }
    public void setActualStart(Instant actualStart) { this.actualStart = actualStart; }
    public Instant getActualEnd() { return actualEnd; }
    public void setActualEnd(Instant actualEnd) { this.actualEnd = actualEnd; }
    public BigDecimal getTolls() { return tolls; }
    public void setTolls(BigDecimal tolls) { this.tolls = tolls; }
    public BigDecimal getParking() { return parking; }
    public void setParking(BigDecimal parking) { this.parking = parking; }
    public BigDecimal getAdditionalCharges() { return additionalCharges; }
    public void setAdditionalCharges(BigDecimal additionalCharges) { this.additionalCharges = additionalCharges; }
    public BigDecimal getBillableAmount() { return billableAmount; }
    public void setBillableAmount(BigDecimal billableAmount) { this.billableAmount = billableAmount; }
}
