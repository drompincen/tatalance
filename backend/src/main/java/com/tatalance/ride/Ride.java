package com.tatalance.ride;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ride extends Job (Category A refactor for Issue #93).
 * Ride-specific: destination locations + driver assignment + waiting/payout.
 * Collection is now "jobs" (inherited from Job) + type="RIDE".
 * pickupDateTime kept for full backward compat with existing API responses, JS, old data, and queries.
 */
public class Ride extends Job {

    @NotBlank
    private String pickupLocation;

    @NotBlank
    private String dropoffLocation;

    private String assignedDriverId;
    private String assignedDriverName;

    private Integer waitingTimeMinutes;

    // Driver payout (#87)
    private BigDecimal driverPayout;
    private boolean payoutPaid;

    public Ride() {
        setType("RIDE");
    }

    // --- Ride-specific getters/setters ---

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }

    public String getDropoffLocation() { return dropoffLocation; }
    public void setDropoffLocation(String dropoffLocation) { this.dropoffLocation = dropoffLocation; }

    public String getAssignedDriverId() { return assignedDriverId; }
    public void setAssignedDriverId(String assignedDriverId) { this.assignedDriverId = assignedDriverId; }

    public String getAssignedDriverName() { return assignedDriverName; }
    public void setAssignedDriverName(String assignedDriverName) { this.assignedDriverName = assignedDriverName; }

    public Integer getWaitingTimeMinutes() { return waitingTimeMinutes; }
    public void setWaitingTimeMinutes(Integer waitingTimeMinutes) { this.waitingTimeMinutes = waitingTimeMinutes; }

    public BigDecimal getDriverPayout() { return driverPayout; }
    public void setDriverPayout(BigDecimal driverPayout) { this.driverPayout = driverPayout; }

    public boolean isPayoutPaid() { return payoutPaid; }
    public void setPayoutPaid(boolean payoutPaid) { this.payoutPaid = payoutPaid; }

    // --- PickupDateTime compatibility layer (for existing UI, tests, old data, driver queue etc) ---
    // Backs onto Job.scheduledTime. Allows JSON to continue using "pickupDateTime".
    // Also @Field helps with legacy mongo docs containing "pickupDateTime".

    @Field("pickupDateTime")
    public Instant getPickupDateTime() {
        Instant st = getScheduledTime();
        return st != null ? st : null;
    }

    public void setPickupDateTime(Instant pickupDateTime) {
        setScheduledTime(pickupDateTime);
    }

    // Ensure type always RIDE even if set externally
    @Override
    public void setType(String type) {
        super.setType("RIDE");
    }
}
