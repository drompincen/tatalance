package com.tatalance.driver;

import com.tatalance.ride.RideRepository;
import com.tatalance.ride.RideStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    public DriverController(DriverRepository repository, RideRepository rideRepository) {
        this.repository = repository;
        this.rideRepository = rideRepository;
    }

    @Operation(summary = "List all drivers")
    @ApiResponse(responseCode = "200", description = "Driver list")
    @GetMapping
    public List<Driver> list() {
        return repository.findAll();
    }

    @Operation(summary = "Get driver by id")
    @ApiResponse(responseCode = "200", description = "Driver found")
    @ApiResponse(responseCode = "404", description = "Driver not found")
    @GetMapping("/{id}")
    public Driver getById(@PathVariable String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
    }

    @Operation(summary = "Create a driver")
    @ApiResponse(responseCode = "201", description = "Driver created")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Driver create(@Valid @RequestBody Driver driver) {
        driver.setCreatedAt(Instant.now());
        return repository.save(driver);
    }

    @Operation(summary = "Update a driver")
    @ApiResponse(responseCode = "200", description = "Driver updated")
    @ApiResponse(responseCode = "404", description = "Driver not found")
    @PutMapping("/{id}")
    public Driver update(@PathVariable String id, @Valid @RequestBody Driver updates) {
        Driver existing = repository.findById(id)
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
        if (!repository.existsById(id)) {
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
        Driver driver = repository.findById(id)
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
