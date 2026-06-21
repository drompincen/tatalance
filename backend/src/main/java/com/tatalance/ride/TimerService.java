package com.tatalance.ride;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TimerService {

    public void startTimer(Ride ride) {
        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot start timer on a " + ride.getStatus() + " job");
        }
        if (hasOpenSegment(ride)) {
            return;
        }
        if (ride.getWorkSegments() == null) {
            ride.setWorkSegments(new ArrayList<>());
        }
        if (ride.getActualStart() == null) {
            ride.setActualStart(Instant.now());
        }
        ride.getWorkSegments().add(new WorkSegment(Instant.now(), null));
        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            ride.setStatus(RideStatus.IN_PROGRESS);
            ride.addStatusEvent(RideStatus.IN_PROGRESS);
        }
    }

    public void pauseTimer(Ride ride) {
        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Timer is not running — status is " + ride.getStatus());
        }
        if (!hasOpenSegment(ride)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No open timer segment to pause");
        }
        closeOpenSegment(ride, Instant.now());
        ride.setStatus(RideStatus.PAUSED);
        ride.addStatusEvent(RideStatus.PAUSED);
    }

    public void resumeTimer(Ride ride) {
        if (ride.getStatus() != RideStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Job is not paused — status is " + ride.getStatus());
        }
        if (ride.getWorkSegments() == null) {
            ride.setWorkSegments(new ArrayList<>());
        }
        ride.getWorkSegments().add(new WorkSegment(Instant.now(), null));
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.addStatusEvent(RideStatus.IN_PROGRESS);
    }

    public void closeOpenSegment(Ride ride, Instant end) {
        if (ride.getWorkSegments() == null) return;
        for (int i = ride.getWorkSegments().size() - 1; i >= 0; i--) {
            WorkSegment seg = ride.getWorkSegments().get(i);
            if (seg.isOpen()) {
                seg.setEndedAt(end);
                return;
            }
        }
    }

    public boolean hasOpenSegment(Ride ride) {
        if (ride.getWorkSegments() == null) return false;
        return ride.getWorkSegments().stream().anyMatch(WorkSegment::isOpen);
    }

    public long workedSeconds(Ride ride, Instant now) {
        if (ride.getWorkSegments() == null || ride.getWorkSegments().isEmpty()) {
            if (ride.getActualStart() != null && ride.getStatus() == RideStatus.IN_PROGRESS) {
                return Duration.between(ride.getActualStart(), now).getSeconds();
            }
            return 0;
        }
        long total = 0;
        for (WorkSegment seg : ride.getWorkSegments()) {
            if (seg.getStartedAt() == null) continue;
            Instant end = seg.getEndedAt() != null ? seg.getEndedAt() : now;
            total += Duration.between(seg.getStartedAt(), end).getSeconds();
        }
        return Math.max(0, total);
    }

    public BigDecimal billableAmount(Ride ride, Instant now) {
        PricingMode mode = ride.getPricingMode() == null ? PricingMode.FLAT : ride.getPricingMode();
        long seconds = workedSeconds(ride, now);
        BigDecimal base = ride.getBasePrice() == null ? BigDecimal.ZERO : ride.getBasePrice();
        BigDecimal timeCost = BigDecimal.ZERO;
        if (mode != PricingMode.FLAT && ride.getHourlyRate() != null && seconds > 0) {
            BigDecimal hours = BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);
            timeCost = ride.getHourlyRate().multiply(hours).setScale(2, RoundingMode.HALF_UP);
        }
        return switch (mode) {
            case HOURLY -> timeCost;
            case FLAT_PLUS_HOURLY -> base.add(timeCost);
            default -> base;
        };
    }

    public Map<String, Object> timerState(Ride ride) {
        Instant now = Instant.now();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("rideId", ride.getId());
        state.put("status", ride.getStatus());
        state.put("running", ride.getStatus() == RideStatus.IN_PROGRESS && hasOpenSegment(ride));
        state.put("workedSeconds", workedSeconds(ride, now));
        state.put("billableAmount", billableAmount(ride, now));
        state.put("hourlyRate", ride.getHourlyRate());
        state.put("jobTitle", ride.getJobTitle());
        state.put("clientName", ride.getClientName());
        List<Map<String, Object>> segments = new ArrayList<>();
        if (ride.getWorkSegments() != null) {
            for (WorkSegment seg : ride.getWorkSegments()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("startedAt", seg.getStartedAt());
                m.put("endedAt", seg.getEndedAt());
                segments.add(m);
            }
        }
        state.put("segments", segments);
        return state;
    }

    public long finalizeDurationMinutes(Ride ride, Instant end) {
        closeOpenSegment(ride, end);
        long seconds = workedSeconds(ride, end);
        long mins = seconds / 60;
        ride.setDurationMinutes(mins);
        ride.setActualEnd(end);
        return mins;
    }
}