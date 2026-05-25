package com.tatalance.ride;

import com.tatalance.client.Client;
import com.tatalance.client.ClientRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Tag(name = "Rides", description = "Ride booking and management")
@RestController
@RequestMapping("/api")
public class RideController {

    private final RideRepository rideRepository;
    private final ClientRepository clientRepository;

    public RideController(RideRepository rideRepository, ClientRepository clientRepository) {
        this.rideRepository = rideRepository;
        this.clientRepository = clientRepository;
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

    @Operation(summary = "List rides assigned to a driver",
            description = "Returns rides where assignedDriverId matches, sorted by pickupDateTime ascending. "
                    + "Powers the mobile driver queue (issue #33).")
    @ApiResponse(responseCode = "200", description = "Driver's rides")
    @GetMapping("/drivers/{driverId}/rides")
    public List<Ride> listByDriver(@PathVariable String driverId) {
        return rideRepository.findByAssignedDriverIdOrderByPickupDateTimeAsc(driverId);
    }
}
