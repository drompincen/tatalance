package com.tatalance.ride;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TimerServiceTest {

    private final TimerService timer = new TimerService();

    @Test
    void pauseResume_tracksSegmentsAndBillable() {
        Ride ride = new Ride();
        ride.setPricingMode(PricingMode.HOURLY);
        ride.setHourlyRate(new BigDecimal("20.00"));
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setWorkSegments(new ArrayList<>());

        timer.startTimer(ride);
        assertEquals(RideStatus.IN_PROGRESS, ride.getStatus());
        assertEquals(1, ride.getWorkSegments().size());
        assertTrue(ride.getWorkSegments().get(0).isOpen());

        Instant t0 = ride.getWorkSegments().get(0).getStartedAt();
        ride.getWorkSegments().get(0).setStartedAt(t0.minusSeconds(3600));
        timer.pauseTimer(ride);
        assertEquals(RideStatus.PAUSED, ride.getStatus());
        assertFalse(ride.getWorkSegments().get(0).isOpen());

        timer.resumeTimer(ride);
        assertEquals(RideStatus.IN_PROGRESS, ride.getStatus());
        assertEquals(2, ride.getWorkSegments().size());

        BigDecimal billable = timer.billableAmount(ride, Instant.now());
        assertTrue(billable.compareTo(new BigDecimal("19.00")) >= 0);
    }
}