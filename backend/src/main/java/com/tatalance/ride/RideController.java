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

    @Operation(summary = "Start a ride (mobile driver action)",
            description = "Transitions SCHEDULED or ACCEPTED → IN_PROGRESS and records actualStart=now. "
                    + "Powers the Start button on the driver queue (issue #34).")
    @PostMapping("/rides/{id}/start")
    public Ride start(@PathVariable String id) {
        Ride ride = rideRepository.findById(id)
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
        Ride ride = rideRepository.findById(id)
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
        ride.setBillableAmount(base.add(tolls).add(parking).add(extras));
        ride.setActualEnd(Instant.now());
        ride.setStatus(RideStatus.COMPLETED);
        return rideRepository.save(ride);
    }

    private static BigDecimal asDecimal(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) return BigDecimal.ZERO;
        Object v = body.get(key);
        if (v instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(v.toString());
    }
}
