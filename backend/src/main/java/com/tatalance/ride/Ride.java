package com.tatalance.ride;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    /** Freelance job title (issue #93); optional for chauffeur rides. */
    private String jobTitle;

    private String assignedDriverId;
    private String assignedDriverName;

    private Integer waitingTimeMinutes;

    // Driver payout (#87)
    private BigDecimal driverPayout;
    private boolean payoutPaid;

    // Freelance timer segments (pause/resume with audit trail)
    private List<WorkSegment> workSegments = new ArrayList<>();

    public Ride() {
        setType("RIDE");
    }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }

    public String getDropoffLocation() { return dropoffLocation; }
    public void setDropoffLocation(String dropoffLocation) { this.dropoffLocation = dropoffLocation; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

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

    public List<WorkSegment> getWorkSegments() { return workSegments; }
    public void setWorkSegments(List<WorkSegment> workSegments) { this.workSegments = workSegments; }

    // --- PickupDateTime compatibility layer (for existing UI, tests, old data, driver queue etc) ---

    @Field("pickupDateTime")
    public Instant getPickupDateTime() {
        Instant st = getScheduledTime();
        return st != null ? st : null;
    }

    public void setPickupDateTime(Instant pickupDateTime) {
        setScheduledTime(pickupDateTime);
    }

    @Override
    public void setType(String type) {
        super.setType("RIDE");
    }
}