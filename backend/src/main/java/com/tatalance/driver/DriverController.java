package com.tatalance.driver;

import com.tatalance.ride.RideRepository;
import com.tatalance.ride.RideStatus;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Tag(name = "Drivers", description = "Driver management")
@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverRepository repository;
    private final RideRepository rideRepository;
    private final AuthHelper authHelper;

    public DriverController(DriverRepository repository, RideRepository rideRepository, AuthHelper authHelper) {
        this.repository = repository;
        this.rideRepository = rideRepository;
        this.authHelper = authHelper;
    }

    @Operation(summary = "List all drivers")
    @ApiResponse(responseCode = "200", description = "Driver list")
    @GetMapping
    public Page<Driver> list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return repository.findByUserId(authHelper.getCurrentUserId(), pageable);
    }

    @Operation(summary = "Get driver by id")
    @ApiResponse(responseCode = "200", description = "Driver found")
    @ApiResponse(responseCode = "404", description = "Driver not found")
    @GetMapping("/{id}")
    public Driver getById(@PathVariable String id) {
        return repository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
    }

    @Operation(summary = "Create a driver")
    @ApiResponse(responseCode = "201", description = "Driver created")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Driver create(@Valid @RequestBody Driver driver) {
        driver.setUserId(authHelper.getCurrentUserId());
        driver.setCreatedAt(Instant.now());
        return repository.save(driver);
    }

    @Operation(summary = "Update a driver")
    @ApiResponse(responseCode = "200", description = "Driver updated")
    @ApiResponse(responseCode = "404", description = "Driver not found")
    @PutMapping("/{id}")
    public Driver update(@PathVariable String id, @Valid @RequestBody Driver updates) {
        Driver existing = repository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        existing.setFirstName(updates.getFirstName());
        existing.setLastName(updates.getLastName());
        existing.setPhone(updates.getPhone());
        existing.setEmail(updates.getEmail());
        existing.setVehicle(updates.getVehicle());
        existing.setPayoutType(updates.getPayoutType());
        existing.setPayoutRate(updates.getPayoutRate());
        return repository.save(existing);
    }

    @Operation(summary = "Delete a driver")
    @ApiResponse(responseCode = "204", description = "Driver deleted")
    @ApiResponse(responseCode = "400", description = "Driver has active rides")
    @ApiResponse(responseCode = "404", description = "Driver not found")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!repository.existsByIdAndUserId(id, authHelper.getCurrentUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found");
        }
        var activeRides = rideRepository.findByAssignedDriverIdAndStatusIn(id,
                List.of(RideStatus.ASSIGNED, RideStatus.IN_PROGRESS));
        if (!activeRides.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete driver with active rides (" + activeRides.size() + " active)");
        }
        repository.deleteById(id);
    }

    @Operation(summary = "Update driver availability")
    @ApiResponse(responseCode = "200", description = "Availability updated")
    @ApiResponse(responseCode = "404", description = "Driver not found")
    @PatchMapping("/{id}/availability")
    public Driver updateAvailability(@PathVariable String id, @RequestBody Map<String, String> body) {
        Driver driver = repository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        String availability = body.get("availability");
        if (availability == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "availability is required");
        }
        try {
            driver.setAvailability(Availability.valueOf(availability.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid availability. Must be one of: AVAILABLE, ON_TRIP, OFF_DUTY");
        }
        return repository.save(driver);
    }
}
