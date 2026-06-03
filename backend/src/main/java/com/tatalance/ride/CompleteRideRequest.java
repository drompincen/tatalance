package com.tatalance.ride;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public class CompleteRideRequest {
    @NotNull
    private Instant actualStart;
    @NotNull
    private Instant actualEnd;
    private Integer waitingTimeMinutes;
    private BigDecimal tolls;
    private BigDecimal parking;
    private BigDecimal additionalCharges;
    private String chargeDescription;

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
}
