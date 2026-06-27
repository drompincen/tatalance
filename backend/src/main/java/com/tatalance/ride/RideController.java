package com.tatalance.ride;

import com.tatalance.activity.ActivityLogger;
import com.tatalance.client.Client;
import com.tatalance.client.ClientRepository;
import com.tatalance.driver.Availability;
import com.tatalance.driver.Driver;
import com.tatalance.driver.DriverRepository;
import com.tatalance.profile.ProfileRepository;
import com.tatalance.user.AuthHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "Rides / Jobs", description = "Ride and unified Job (service/freelance) booking and management. Category A refactor #93: base Job + Ride extends, collection=jobs")
@RestController
@RequestMapping("/api")
public class RideController {

    private final RideRepository rideRepository;
    private final ClientRepository clientRepository;
    private final DriverRepository driverRepository;
    private final AuthHelper authHelper;
    private final ActivityLogger activityLog;
    private final TimerService timerService;
    private final ProfileRepository profileRepository;

    public RideController(RideRepository rideRepository, ClientRepository clientRepository,
                          DriverRepository driverRepository, AuthHelper authHelper,
                          ActivityLogger activityLog, TimerService timerService,
                          ProfileRepository profileRepository) {
        this.rideRepository = rideRepository;
        this.clientRepository = clientRepository;
        this.driverRepository = driverRepository;
        this.authHelper = authHelper;
        this.activityLog = activityLog;
        this.timerService = timerService;
        this.profileRepository = profileRepository;
    }

    @Operation(summary = "Create a ride")
    @ApiResponse(responseCode = "201", description = "Ride created")
    @PostMapping("/rides")
    @ResponseStatus(HttpStatus.CREATED)
    public Ride create(@Valid @RequestBody Ride ride, @RequestParam(required = false) String profileId) {
        if (ride.getPickupDateTime() != null && ride.getPickupDateTime().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup date/time cannot be in the past");
        }
        String userId = authHelper.getCurrentUserId();
        if (profileId != null && !profileId.isBlank()) {
            profileRepository.findByIdAndUserId(profileId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile not found or does not belong to user"));
            ride.setProfileId(profileId);
        }
        Client client = clientRepository.findByIdAndUserId(ride.getClientId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));
        ride.setUserId(userId);
        ride.setClientName(client.getFirstName() + " " + client.getLastName());
        ride.setStatus(RideStatus.SCHEDULED);
        ride.addStatusEvent(RideStatus.SCHEDULED);
        ride.setCreatedAt(Instant.now());
        Ride saved = rideRepository.save(ride);
        activityLog.log(userId, "CREATE", "Ride", saved.getId(),
                "Booked ride for " + saved.getClientName() + (profileId != null ? " under profile " + profileId : ""));
        return saved;
    }

    @Operation(summary = "Create a service/freelance Job (base Job, for developer jobs etc, Issue #93). Link to profile for multi-profile support.")
    @ApiResponse(responseCode = "201", description = "Job created")
    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public Job createJob(@RequestBody Job job, @RequestParam(required = false) String profileId) {
        // For base Job (SERVICE) use case. No destination validation. scheduledTime optional for MVP.
        String userId = authHelper.getCurrentUserId();
        if (job.getClientId() != null) {
            Client client = clientRepository.findByIdAndUserId(job.getClientId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));
            job.setClientName(client.getFirstName() + " " + client.getLastName());
        }
        job.setUserId(userId);
        if (profileId != null && !profileId.isBlank()) {
            profileRepository.findByIdAndUserId(profileId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile not found or does not belong to user"));
            job.setProfileId(profileId);
        }
        if (job.getType() == null || job.getType().isBlank()) {
            job.setType("SERVICE");
        }
        job.setStatus(RideStatus.SCHEDULED);
        job.addStatusEvent(RideStatus.SCHEDULED);
        job.setCreatedAt(Instant.now());
        // Note: save via rideRepository (which extends JobRepository) works for base Job too
        Job saved = rideRepository.save(job);
        activityLog.log(userId, "CREATE", "Job", saved.getId(),
                "Booked job for " + saved.getClientName() + (profileId != null ? " under profile " + profileId : ""));
        return saved;
    }

    @Operation(summary = "List all rides")
    @ApiResponse(responseCode = "200", description = "Ride list")
    @GetMapping("/rides")
    public Page<Ride> list(@RequestParam(required = false) String profileId,
                           @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = authHelper.getCurrentUserId();
        if (profileId != null && !profileId.isBlank()) {
            // validate ownership
            profileRepository.findByIdAndUserId(profileId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile not found or does not belong to user"));
            return rideRepository.findByUserIdAndProfileId(userId, profileId, pageable);
        }
        return rideRepository.findByUserId(userId, pageable);
    }

    @Operation(summary = "Get ride by id")
    @ApiResponse(responseCode = "200", description = "Ride found")
    @ApiResponse(responseCode = "404", description = "Ride not found")
    @GetMapping("/rides/{id}")
    public Ride getById(@PathVariable String id) {
        return rideRepository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
    }

    @Operation(summary = "List rides for a client")
    @ApiResponse(responseCode = "200", description = "Client rides")
    @GetMapping("/clients/{clientId}/rides")
    public List<Ride> listByClient(@PathVariable String clientId) {
        String userId = authHelper.getCurrentUserId();
        if (!clientRepository.existsByIdAndUserId(clientId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        return rideRepository.findByUserIdAndClientId(userId, clientId);
    }

    @Operation(summary = "List rides assigned to a driver",
            description = "Returns rides where assignedDriverId matches, sorted by pickupDateTime ascending. "
                    + "Powers the mobile driver queue (issue #33).")
    @ApiResponse(responseCode = "200", description = "Driver's rides")
    @GetMapping("/drivers/{driverId}/rides")
    public List<Ride> listByDriver(@PathVariable String driverId) {
        String userId = authHelper.getCurrentUserId();
        if (!driverRepository.existsByIdAndUserId(driverId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found");
        }
        return rideRepository.findByUserIdAndAssignedDriverIdOrderByPickupDateTimeAsc(userId, driverId);
    }

    @Operation(summary = "Assign a driver to a ride")
    @ApiResponse(responseCode = "200", description = "Driver assigned")
    @ApiResponse(responseCode = "400", description = "Driver not available or not found")
    @ApiResponse(responseCode = "404", description = "Ride not found")
    @PostMapping("/rides/{id}/assign")
    public Ride assignDriver(@PathVariable String id, @RequestBody Map<String, String> body) {
        String userId = authHelper.getCurrentUserId();
        Ride ride = rideRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));

        String driverId = body.get("driverId");
        if (driverId == null || driverId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "driverId is required");
        }

        Driver driver = driverRepository.findByIdAndUserId(driverId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver not found"));

        if (driver.getAvailability() != Availability.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver is not available");
        }

        // Free previously assigned driver if reassigning
        if (ride.getAssignedDriverId() != null) {
            driverRepository.findByIdAndUserId(ride.getAssignedDriverId(), userId).ifPresent(prev -> {
                prev.setAvailability(Availability.AVAILABLE);
                driverRepository.save(prev);
            });
        }

        ride.setAssignedDriverId(driverId);
        ride.setAssignedDriverName(driver.getFirstName() + " " + driver.getLastName());
        ride.setStatus(RideStatus.ASSIGNED);
        ride.addStatusEvent(RideStatus.ASSIGNED);

        driver.setAvailability(Availability.ON_TRIP);
        driverRepository.save(driver);

        return rideRepository.save(ride);
    }

    @Operation(summary = "Update a scheduled ride")
    @ApiResponse(responseCode = "200", description = "Ride updated")
    @ApiResponse(responseCode = "400", description = "Ride not in SCHEDULED status")
    @ApiResponse(responseCode = "404", description = "Ride not found")
    @PutMapping("/rides/{id}")
    public Ride update(@PathVariable String id, @Valid @RequestBody Ride updates) {
        String userId = authHelper.getCurrentUserId();
        Ride existing = rideRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (existing.getStatus() != RideStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only SCHEDULED rides can be edited");
        }
        if (updates.getPickupDateTime() != null && updates.getPickupDateTime().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup date/time cannot be in the past");
        }
        Client client = clientRepository.findByIdAndUserId(updates.getClientId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));
        existing.setClientId(updates.getClientId());
        existing.setClientName(client.getFirstName() + " " + client.getLastName());
        existing.setPickupDateTime(updates.getPickupDateTime());
        existing.setPickupLocation(updates.getPickupLocation());
        existing.setDropoffLocation(updates.getDropoffLocation());
        existing.setBasePrice(updates.getBasePrice());
        existing.setPricingMode(updates.getPricingMode());
        existing.setHourlyRate(updates.getHourlyRate());
        existing.setNotes(updates.getNotes());
        if (updates.getJobTitle() != null) {
            existing.setJobTitle(updates.getJobTitle());
        }
        return rideRepository.save(existing);
    }

    @Operation(summary = "Cancel a ride")
    @ApiResponse(responseCode = "200", description = "Ride cancelled")
    @ApiResponse(responseCode = "400", description = "Ride cannot be cancelled")
    @ApiResponse(responseCode = "404", description = "Ride not found")
    @PostMapping("/rides/{id}/cancel")
    public Ride cancelRide(@PathVariable String id) {
        String userId = authHelper.getCurrentUserId();
        Ride ride = rideRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel a " + ride.getStatus() + " ride");
        }
        // Free the assigned driver
        if (ride.getAssignedDriverId() != null) {
            driverRepository.findByIdAndUserId(ride.getAssignedDriverId(), userId).ifPresent(driver -> {
                driver.setAvailability(Availability.AVAILABLE);
                driverRepository.save(driver);
            });
        }
        ride.setStatus(RideStatus.CANCELLED);
        ride.addStatusEvent(RideStatus.CANCELLED);
        Ride saved = rideRepository.save(ride);
        activityLog.log(userId, "CANCEL", "Ride", id, "Cancelled ride for " + ride.getClientName());
        return saved;
    }

    @Operation(summary = "Start a ride (mobile driver action)",
            description = "Transitions SCHEDULED or ACCEPTED → IN_PROGRESS and records actualStart=now. "
                    + "Powers the Start button on the driver queue (issue #34).")
    @PostMapping("/rides/{id}/start")
    public Ride start(@PathVariable String id) {
        Ride ride = rideRepository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED
                || ride.getStatus() == RideStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot start a ride in status " + ride.getStatus());
        }
        timerService.startTimer(ride);
        return rideRepository.save(ride);
    }

    @Operation(summary = "Pause freelance timer", description = "Closes the open work segment and sets status PAUSED.")
    @PostMapping("/rides/{id}/timer/pause")
    public Ride pauseTimer(@PathVariable String id) {
        Ride ride = rideRepository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        timerService.pauseTimer(ride);
        return rideRepository.save(ride);
    }

    @Operation(summary = "Resume freelance timer", description = "Opens a new work segment after PAUSED.")
    @PostMapping("/rides/{id}/timer/resume")
    public Ride resumeTimer(@PathVariable String id) {
        Ride ride = rideRepository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        timerService.resumeTimer(ride);
        return rideRepository.save(ride);
    }

    @Operation(summary = "Timer state for recovery", description = "Server-side worked seconds and billable amount from work segments.")
    @GetMapping("/rides/{id}/timer")
    public Map<String, Object> timerState(@PathVariable String id) {
        Ride ride = rideRepository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        return timerService.timerState(ride);
    }

    @Operation(summary = "Complete a ride with billable extras (mobile driver action)",
            description = "Transitions IN_PROGRESS → COMPLETED. Body may include tolls / parking / "
                    + "additionalCharges; billableAmount = basePrice + extras. Issue #34.")
    @PostMapping("/rides/{id}/complete")
    public Ride complete(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        String userId = authHelper.getCurrentUserId();
        Ride ride = rideRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));

        Instant end;
        if (ride.getStatus() == RideStatus.ASSIGNED) {
            Instant start = asInstant(body, "actualStart");
            end = asInstant(body, "actualEnd");
            if (start == null || end == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "actualStart and actualEnd are required to complete an assigned ride");
            }
            if (!end.isAfter(start)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "actualEnd must be after actualStart");
            }
            ride.setActualStart(start);
            ride.setActualEnd(end);
            ride.setWorkSegments(new ArrayList<>(List.of(new WorkSegment(start, end))));
            ride.setDurationMinutes(Duration.between(start, end).toMinutes());
        } else if (ride.getStatus() == RideStatus.IN_PROGRESS || ride.getStatus() == RideStatus.PAUSED) {
            end = Instant.now();
            timerService.finalizeDurationMinutes(ride, end);
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot complete a ride in status " + ride.getStatus() + " — start the timer first");
        }

        BigDecimal tolls   = asDecimal(body, "tolls");
        BigDecimal parking = asDecimal(body, "parking");
        BigDecimal extras  = asDecimal(body, "additionalCharges");
        BigDecimal base    = ride.getBasePrice() == null ? BigDecimal.ZERO : ride.getBasePrice();
        ride.setTolls(tolls);
        ride.setParking(parking);
        ride.setAdditionalCharges(extras);

        PricingMode mode = ride.getPricingMode() == null ? PricingMode.FLAT : ride.getPricingMode();
        BigDecimal total = timerService.billableAmount(ride, end);
        if (mode == PricingMode.FLAT && total.compareTo(BigDecimal.ZERO) == 0) {
            total = base;
        }
        BigDecimal billable = total.add(tolls).add(parking).add(extras);
        ride.setBillableAmount(billable);
        ride.setTotalAmount(total);

        // Calculate driver payout (#87)
        if (ride.getAssignedDriverId() != null) {
            driverRepository.findByIdAndUserId(ride.getAssignedDriverId(), userId)
                    .ifPresent(driver -> {
                        if (driver.getPayoutType() != null && driver.getPayoutRate() != null) {
                            BigDecimal payout = switch (driver.getPayoutType()) {
                                case FLAT -> driver.getPayoutRate();
                                case PERCENTAGE -> billable.multiply(driver.getPayoutRate())
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            };
                            ride.setDriverPayout(payout);
                        }
                        driver.setAvailability(Availability.AVAILABLE);
                        driverRepository.save(driver);
                    });
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.addStatusEvent(RideStatus.COMPLETED);
        Ride saved = rideRepository.save(ride);
        activityLog.log(userId, "COMPLETE", "Ride", id,
                "Completed ride for " + ride.getClientName() + " — $" + total);
        return saved;
    }

    private static Instant asInstant(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        Object v = body.get(key);
        return Instant.parse(v.toString());
    }

    private static BigDecimal asDecimal(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) return BigDecimal.ZERO;
        Object v = body.get(key);
        if (v instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(v.toString());
    }

    @Operation(summary = "Mark driver payout as paid")
    @ApiResponse(responseCode = "200", description = "Payout marked as paid")
    @ApiResponse(responseCode = "404", description = "Ride not found")
    @PostMapping("/rides/{id}/mark-payout-paid")
    public Ride markPayoutPaid(@PathVariable String id) {
        String userId = authHelper.getCurrentUserId();
        Ride ride = rideRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only completed rides can have payouts marked");
        }
        ride.setPayoutPaid(true);
        Ride saved = rideRepository.save(ride);
        activityLog.log(userId, "UPDATE", "Ride", id,
                "Marked payout paid for " + ride.getClientName()
                        + (ride.getDriverPayout() != null ? " — $" + ride.getDriverPayout() : ""));
        return saved;
    }

    @Operation(summary = "List available drivers")
    @ApiResponse(responseCode = "200", description = "Available drivers")
    @GetMapping("/drivers/available")
    public List<Driver> listAvailableDrivers() {
        return driverRepository.findByUserIdAndAvailability(
                authHelper.getCurrentUserId(), Availability.AVAILABLE);
    }
}
