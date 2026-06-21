package com.tatalance.ride;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "rides")
public class Ride {
    @Id
    private String id;
    @Indexed
    private String userId;
    @NotBlank
    private String clientId;
    private String clientName;
    @NotNull
    private Instant pickupDateTime;
    @NotBlank
    private String pickupLocation;
    @NotBlank
    private String dropoffLocation;
    /** Freelance job title (issue #93); optional for chauffeur rides. */
    private String jobTitle;
    private BigDecimal basePrice;
    private PricingMode pricingMode;
    private BigDecimal hourlyRate;
    private String notes;
    private RideStatus status = RideStatus.SCHEDULED;
    private String assignedDriverId;
    private String assignedDriverName;
    private Instant createdAt;

    // Completion details (set by POST /api/rides/{id}/start and /complete — M4, #34)
    private Instant actualStart;
    private Instant actualEnd;
    private Integer waitingTimeMinutes;
    private BigDecimal tolls;
    private BigDecimal parking;
    private BigDecimal additionalCharges;
    private String chargeDescription;
    private Long durationMinutes;
    private BigDecimal totalAmount;
    private BigDecimal billableAmount;

    // Driver payout (#87)
    private BigDecimal driverPayout;
    private boolean payoutPaid;

    // Freelance timer segments (pause/resume with audit trail)
    private List<WorkSegment> workSegments = new ArrayList<>();

    // Status history (#88)
    private List<StatusEvent> statusHistory = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
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
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public PricingMode getPricingMode() { return pricingMode; }
    public void setPricingMode(PricingMode pricingMode) { this.pricingMode = pricingMode; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public RideStatus getStatus() { return status; }
    public void setStatus(RideStatus status) { this.status = status; }
    public String getAssignedDriverId() { return assignedDriverId; }
    public void setAssignedDriverId(String assignedDriverId) { this.assignedDriverId = assignedDriverId; }
    public String getAssignedDriverName() { return assignedDriverName; }
    public void setAssignedDriverName(String assignedDriverName) { this.assignedDriverName = assignedDriverName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getActualStart() { return actualStart; }
    public void setActualStart(Instant actualStart) { this.actualStart = actualStart; }
    public Instant getActualEnd() { return actualEnd; }
    public void setActualEnd(Instant actualEnd) { this.actualEnd = actualEnd; }
    public Integer getWaitingTimeMinutes() { return waitingTimeMinutes; }
    public void setWaitingTimeMinutes(Integer waitingTimeMinutes) { this.waitingTimeMinutes = waitingTimeMinutes; }
    public BigDecimal getTolls() { return tolls; }
    public void setTolls(BigDecimal tolls) { this.tolls = tolls; }
    public BigDecimal getParking() { return parking; }
    public void setParking(BigDecimal parking) { this.parking = parking; }
    public BigDecimal getAdditionalCharges() { return additionalCharges; }
    public void setAdditionalCharges(BigDecimal additionalCharges) { this.additionalCharges = additionalCharges; }
    public String getChargeDescription() { return chargeDescription; }
    public void setChargeDescription(String chargeDescription) { this.chargeDescription = chargeDescription; }
    public Long getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Long durationMinutes) { this.durationMinutes = durationMinutes; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getBillableAmount() { return billableAmount; }
    public void setBillableAmount(BigDecimal billableAmount) { this.billableAmount = billableAmount; }
    public BigDecimal getDriverPayout() { return driverPayout; }
    public void setDriverPayout(BigDecimal driverPayout) { this.driverPayout = driverPayout; }
    public boolean isPayoutPaid() { return payoutPaid; }
    public void setPayoutPaid(boolean payoutPaid) { this.payoutPaid = payoutPaid; }
    public List<WorkSegment> getWorkSegments() { return workSegments; }
    public void setWorkSegments(List<WorkSegment> workSegments) { this.workSegments = workSegments; }
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
        public StatusEvent(RideStatus status, Instant timestamp) { this.status = status; this.timestamp = timestamp; }
        public RideStatus getStatus() { return status; }
        public void setStatus(RideStatus status) { this.status = status; }
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    }
}
