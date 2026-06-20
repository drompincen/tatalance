package com.tatalance.ride;

import com.tatalance.activity.ActivityLogger;
import com.tatalance.client.Client;
import com.tatalance.client.ClientRepository;
import com.tatalance.driver.Availability;
import com.tatalance.driver.Driver;
import com.tatalance.driver.DriverRepository;
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
import java.util.List;
import java.util.Map;

@Tag(name = "Rides", description = "Ride booking and management")
@RestController
@RequestMapping("/api")
public class RideController {

    private final RideRepository rideRepository;
    private final ClientRepository clientRepository;
    private final DriverRepository driverRepository;
    private final AuthHelper authHelper;
    private final ActivityLogger activityLog;

    public RideController(RideRepository rideRepository, ClientRepository clientRepository,
                          DriverRepository driverRepository, AuthHelper authHelper,
                          ActivityLogger activityLog) {
        this.rideRepository = rideRepository;
        this.clientRepository = clientRepository;
        this.driverRepository = driverRepository;
        this.authHelper = authHelper;
        this.activityLog = activityLog;
    }

    @Operation(summary = "Create a ride")
    @ApiResponse(responseCode = "201", description = "Ride created")
    @PostMapping("/rides")
    @ResponseStatus(HttpStatus.CREATED)
    public Ride create(@Valid @RequestBody Ride ride) {
        if (ride.getPickupDateTime() != null && ride.getPickupDateTime().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup date/time cannot be in the past");
        }
        String userId = authHelper.getCurrentUserId();
        Client client = clientRepository.findByIdAndUserId(ride.getClientId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));
        ride.setUserId(userId);
        ride.setClientName(client.getFirstName() + " " + client.getLastName());
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setCreatedAt(Instant.now());
        Ride saved = rideRepository.save(ride);
        activityLog.log(userId, "CREATE", "Ride", saved.getId(),
                "Booked ride for " + saved.getClientName());
        return saved;
    }

    @Operation(summary = "List all rides")
    @ApiResponse(responseCode = "200", description = "Ride list")
    @GetMapping("/rides")
    public Page<Ride> list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return rideRepository.findByUserId(authHelper.getCurrentUserId(), pageable);
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
        return rideRepository.findByClientId(clientId);
    }

    @Operation(summary = "List rides assigned to a driver",
            description = "Returns rides where assignedDriverId matches, sorted by pickupDateTime ascending. "
                    + "Powers the mobile driver queue (issue #33).")
    @ApiResponse(responseCode = "200", description = "Driver's rides")
    @GetMapping("/drivers/{driverId}/rides")
    public List<Ride> listByDriver(@PathVariable String driverId) {
        return rideRepository.findByAssignedDriverIdOrderByPickupDateTimeAsc(driverId);
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
        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot start a ride in status " + ride.getStatus());
        }
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setActualStart(Instant.now());
        return rideRepository.save(ride);
    }

    @Operation(summary = "Complete a ride with billable extras (mobile driver action)",
            description = "Transitions IN_PROGRESS → COMPLETED. Body may include tolls / parking / "
                    + "additionalCharges; billableAmount = basePrice + extras. Issue #34.")
    @PostMapping("/rides/{id}/complete")
    public Ride complete(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        Ride ride = rideRepository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot complete a ride in status " + ride.getStatus() + " — start it first");
        }
        BigDecimal tolls   = asDecimal(body, "tolls");
        BigDecimal parking = asDecimal(body, "parking");
        BigDecimal extras  = asDecimal(body, "additionalCharges");
        BigDecimal base    = ride.getBasePrice() == null ? BigDecimal.ZERO : ride.getBasePrice();
        ride.setTolls(tolls);
        ride.setParking(parking);
        ride.setAdditionalCharges(extras);
        ride.setActualEnd(Instant.now());

        PricingMode mode = ride.getPricingMode() == null ? PricingMode.FLAT : ride.getPricingMode();
        BigDecimal timeCost = BigDecimal.ZERO;
        long durationMins = 0;
        if (ride.getActualStart() != null) {
            durationMins = Duration.between(ride.getActualStart(), ride.getActualEnd()).toMinutes();
            ride.setDurationMinutes(durationMins);
        }
        if (mode != PricingMode.FLAT && ride.getHourlyRate() != null && durationMins > 0) {
            BigDecimal hours = BigDecimal.valueOf(durationMins).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            timeCost = ride.getHourlyRate().multiply(hours).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total;
        switch (mode) {
            case HOURLY -> total = timeCost;
            case FLAT_PLUS_HOURLY -> total = base.add(timeCost);
            default -> total = base;
        }
        ride.setBillableAmount(total.add(tolls).add(parking).add(extras));
        ride.setTotalAmount(total);
        ride.setStatus(RideStatus.COMPLETED);
        Ride saved = rideRepository.save(ride);
        activityLog.log(authHelper.getCurrentUserId(), "COMPLETE", "Ride", id,
                "Completed ride for " + ride.getClientName() + " — $" + total);
        return saved;
    }

    private static BigDecimal asDecimal(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) return BigDecimal.ZERO;
        Object v = body.get(key);
        if (v instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(v.toString());
    }

    @Operation(summary = "List available drivers")
    @ApiResponse(responseCode = "200", description = "Available drivers")
    @GetMapping("/drivers/available")
    public List<Driver> listAvailableDrivers() {
        return driverRepository.findByAvailability(Availability.AVAILABLE);
    }
}
