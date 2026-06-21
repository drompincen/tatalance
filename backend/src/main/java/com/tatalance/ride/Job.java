package com.tatalance.ride;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Base Job model for bookings (Rides and Service/Freelance Jobs).
 * Refactored per Issue #93 Category A.
 *
 * Collection unified to "jobs" with type discriminator ("RIDE" / "SERVICE").
 * Ride extends Job; base Job used directly for developer/freelance service jobs (no destination/driver fields).
 *
 * scheduledTime is the common scheduling field (Ride provides pickupDateTime alias for compat).
 */
@Document(collection = "jobs")
public class Job {

    @Id
    private String id;

    @Indexed
    private String userId;

    /** Profile within the account (owner) that this job belongs to. Clients are shared at account level. */
    @Indexed
    private String profileId;

    private String clientId;
    private String clientName;

    /** Common scheduled time. For legacy rides also exposed as pickupDateTime. */
    private Instant scheduledTime;

    private String notes;

    private BigDecimal basePrice;
    private PricingMode pricingMode;
    private BigDecimal hourlyRate;

    private RideStatus status = RideStatus.SCHEDULED;

    private Instant createdAt;

    // Time tracking / completion (shared)
    private Instant actualStart;
    private Instant actualEnd;
    private Long durationMinutes;
    private BigDecimal totalAmount;
    private BigDecimal billableAmount;
    private BigDecimal additionalCharges;
    private String chargeDescription;
    private BigDecimal tolls;
    private BigDecimal parking;

    // Status history (#88)
    private List<StatusEvent> statusHistory = new ArrayList<>();

    /** Discriminator: "RIDE" or "SERVICE" (for developer/freelance jobs). */
    private String type = "SERVICE";

    // --- Getters/Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProfileId() { return profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public Instant getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(Instant scheduledTime) { this.scheduledTime = scheduledTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public PricingMode getPricingMode() { return pricingMode; }
    public void setPricingMode(PricingMode pricingMode) { this.pricingMode = pricingMode; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public RideStatus getStatus() { return status; }
    public void setStatus(RideStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getActualStart() { return actualStart; }
    public void setActualStart(Instant actualStart) { this.actualStart = actualStart; }

    public Instant getActualEnd() { return actualEnd; }
    public void setActualEnd(Instant actualEnd) { this.actualEnd = actualEnd; }

    public Long getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Long durationMinutes) { this.durationMinutes = durationMinutes; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getBillableAmount() { return billableAmount; }
    public void setBillableAmount(BigDecimal billableAmount) { this.billableAmount = billableAmount; }

    public BigDecimal getAdditionalCharges() { return additionalCharges; }
    public void setAdditionalCharges(BigDecimal additionalCharges) { this.additionalCharges = additionalCharges; }

    public String getChargeDescription() { return chargeDescription; }
    public void setChargeDescription(String chargeDescription) { this.chargeDescription = chargeDescription; }

    public BigDecimal getTolls() { return tolls; }
    public void setTolls(BigDecimal tolls) { this.tolls = tolls; }

    public BigDecimal getParking() { return parking; }
    public void setParking(BigDecimal parking) { this.parking = parking; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<StatusEvent> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<StatusEvent> statusHistory) { this.statusHistory = statusHistory; }

    public void addStatusEvent(RideStatus status) {
        if (this.statusHistory == null) this.statusHistory = new ArrayList<>();
        this.statusHistory.add(new StatusEvent(status, Instant.now()));
    }

    public static class StatusEvent {
        private RideStatus status;
        private Instant timestamp;

        public StatusEvent() {}

        public StatusEvent(RideStatus status, Instant timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }

        public RideStatus getStatus() { return status; }
        public void setStatus(RideStatus status) { this.status = status; }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    }
}
