package com.tatalance.ride;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

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

    @Test
    void startTimer_conflictOnCompleted() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.COMPLETED);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> timer.startTimer(ride));
        assertTrue(ex.getReason().contains("Cannot start timer on a COMPLETED"));
    }

    @Test
    void startTimer_conflictOnCancelled() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.CANCELLED);
        assertThrows(ResponseStatusException.class, () -> timer.startTimer(ride));
    }

    @Test
    void pauseTimer_conflictWhenNotInProgress() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.SCHEDULED);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> timer.pauseTimer(ride));
        assertTrue(ex.getReason().contains("Timer is not running"));
    }

    @Test
    void resumeTimer_conflictWhenNotPaused() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.IN_PROGRESS);
        assertThrows(ResponseStatusException.class, () -> timer.resumeTimer(ride));
    }

    @Test
    void startTimer_whenAlreadyOpenSegment_doesNotAddDuplicate() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setWorkSegments(new ArrayList<>());
        timer.startTimer(ride);
        int sizeAfterFirst = ride.getWorkSegments().size();
        timer.startTimer(ride); // should return early
        assertEquals(sizeAfterFirst, ride.getWorkSegments().size());
    }

    @Test
    void billable_flatMode_ignoresHourly() {
        Ride ride = new Ride();
        ride.setPricingMode(PricingMode.FLAT);
        ride.setBasePrice(new BigDecimal("100"));
        ride.setHourlyRate(new BigDecimal("50"));
        BigDecimal b = timer.billableAmount(ride, Instant.now());
        assertEquals(0, b.compareTo(new BigDecimal("100")));
    }

    @Test
    void workedSeconds_fallbackActualStart_inProgress_coversNullSegmentsAndStatus() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setActualStart(Instant.now().minusSeconds(3600));
        ride.setWorkSegments(null);
        long secs = timer.workedSeconds(ride, Instant.now());
        assertTrue(secs >= 3500 && secs <= 3700);
    }

    @Test
    void billable_hourlyAndFlatPlus_coverIfCalcAndSwitchBranches() {
        Ride ride = new Ride();
        ride.setPricingMode(PricingMode.HOURLY);
        ride.setHourlyRate(new BigDecimal("30.00"));
        ArrayList<WorkSegment> segs = new ArrayList<>();
        segs.add(new WorkSegment(Instant.now().minusSeconds(1800), Instant.now()));
        ride.setWorkSegments(segs);
        BigDecimal b = timer.billableAmount(ride, Instant.now());
        assertTrue(b.compareTo(BigDecimal.ZERO) > 0);

        ride.setPricingMode(PricingMode.FLAT_PLUS_HOURLY);
        ride.setBasePrice(new BigDecimal("50.00"));
        BigDecimal b2 = timer.billableAmount(ride, Instant.now());
        assertTrue(b2.compareTo(new BigDecimal("50")) > 0);
    }

    @Test
    void timerState_finalize_closeNoOpen_coverNullSegmentsIfsAndStateBranches() {
        Ride ride = new Ride();
        ride.setId("r1");
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setWorkSegments(null);
        var state = timer.timerState(ride);
        assertEquals("r1", state.get("rideId"));
        assertNotNull(state.get("workedSeconds"));
        assertFalse((Boolean) state.get("running"));

        // setup then finalize covers close + worked
        ride.setWorkSegments(new ArrayList<>());
        timer.startTimer(ride);
        long mins = timer.finalizeDurationMinutes(ride, Instant.now().plusSeconds(300));
        assertTrue(mins >= 0);
        assertNotNull(ride.getActualEnd());
    }
}