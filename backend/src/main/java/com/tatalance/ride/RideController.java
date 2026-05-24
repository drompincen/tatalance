package com.tatalance.ride;

import com.tatalance.client.Client;
import com.tatalance.client.ClientRepository;
import com.tatalance.driver.Availability;
import com.tatalance.driver.Driver;
import com.tatalance.driver.DriverRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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

    public RideController(RideRepository rideRepository, ClientRepository clientRepository, DriverRepository driverRepository) {
        this.rideRepository = rideRepository;
        this.clientRepository = clientRepository;
        this.driverRepository = driverRepository;
    }

    @Operation(summary = "Create a ride")
    @ApiResponse(responseCode = "201", description = "Ride created")
    @PostMapping("/rides")
    @ResponseStatus(HttpStatus.CREATED)
    public Ride create(@Valid @RequestBody Ride ride) {
        Client client = clientRepository.findById(ride.getClientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));
        ride.setClientName(client.getFirstName() + " " + client.getLastName());
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setCreatedAt(Instant.now());
        return rideRepository.save(ride);
    }

    @Operation(summary = "List all rides")
    @ApiResponse(responseCode = "200", description = "Ride list")
    @GetMapping("/rides")
    public List<Ride> list() {
        return rideRepository.findAll();
    }

    @Operation(summary = "Get ride by id")
    @ApiResponse(responseCode = "200", description = "Ride found")
    @ApiResponse(responseCode = "404", description = "Ride not found")
    @GetMapping("/rides/{id}")
    public Ride getById(@PathVariable String id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
    }

    @Operation(summary = "List rides for a client")
    @ApiResponse(responseCode = "200", description = "Client rides")
    @GetMapping("/clients/{clientId}/rides")
    public List<Ride> listByClient(@PathVariable String clientId) {
        return rideRepository.findByClientId(clientId);
    }

    @Operation(summary = "Assign a driver to a ride")
    @ApiResponse(responseCode = "200", description = "Driver assigned")
    @ApiResponse(responseCode = "400", description = "Driver not available or not found")
    @ApiResponse(responseCode = "404", description = "Ride not found")
    @PostMapping("/rides/{id}/assign")
    public Ride assignDriver(@PathVariable String id, @RequestBody Map<String, String> body) {
        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));

        String driverId = body.get("driverId");
        if (driverId == null || driverId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "driverId is required");
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver not found"));

        if (driver.getAvailability() != Availability.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver is not available");
        }

        // Free previously assigned driver if reassigning
        if (ride.getAssignedDriverId() != null) {
            driverRepository.findById(ride.getAssignedDriverId()).ifPresent(prev -> {
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

    @Operation(summary = "Complete a ride")
    @ApiResponse(responseCode = "200", description = "Ride completed")
    @ApiResponse(responseCode = "400", description = "Ride not in ASSIGNED status or missing fields")
    @ApiResponse(responseCode = "404", description = "Ride not found")
    @PostMapping("/rides/{id}/complete")
    public Ride completeRide(@PathVariable String id, @Valid @RequestBody CompleteRideRequest req) {
        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));

        if (ride.getStatus() != RideStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ride must be in ASSIGNED status to complete");
        }

        ride.setActualStart(req.getActualStart());
        ride.setActualEnd(req.getActualEnd());
        ride.setWaitingTimeMinutes(req.getWaitingTimeMinutes());
        ride.setTolls(req.getTolls());
        ride.setParking(req.getParking());
        ride.setAdditionalCharges(req.getAdditionalCharges());
        ride.setChargeDescription(req.getChargeDescription());

        // Calculate total: basePrice + tolls + parking + additionalCharges
        BigDecimal total = ride.getBasePrice() != null ? ride.getBasePrice() : BigDecimal.ZERO;
        if (req.getTolls() != null) total = total.add(req.getTolls());
        if (req.getParking() != null) total = total.add(req.getParking());
        if (req.getAdditionalCharges() != null) total = total.add(req.getAdditionalCharges());
        ride.setTotalAmount(total);

        ride.setStatus(RideStatus.COMPLETED);

        // Free the driver
        if (ride.getAssignedDriverId() != null) {
            driverRepository.findById(ride.getAssignedDriverId()).ifPresent(driver -> {
                driver.setAvailability(Availability.AVAILABLE);
                driverRepository.save(driver);
            });
        }

        return rideRepository.save(ride);
    }

    @Operation(summary = "List available drivers")
    @ApiResponse(responseCode = "200", description = "Available drivers")
    @GetMapping("/drivers/available")
    public List<Driver> listAvailableDrivers() {
        return driverRepository.findByAvailability(Availability.AVAILABLE);
    }
}
