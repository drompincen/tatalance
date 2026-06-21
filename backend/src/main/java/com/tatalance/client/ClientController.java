package com.tatalance.client;

import com.tatalance.activity.ActivityLogger;
import com.tatalance.ride.JobRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Clients", description = "Client management")
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientRepository repository;
    private final RideRepository rideRepository;
    private final AuthHelper authHelper;
    private final ActivityLogger activityLog;

    public ClientController(ClientRepository repository, RideRepository rideRepository,
                            AuthHelper authHelper, ActivityLogger activityLog) {
        this.repository = repository;
        this.rideRepository = rideRepository;
        this.authHelper = authHelper;
        this.activityLog = activityLog;
    }

    // Updated for #93 Job refactor: active rides check + stats use RideRepository (RIDE filtered); billable now from Job base.

    @Operation(summary = "List all clients")
    @ApiResponse(responseCode = "200", description = "Client list")
    @GetMapping
    public Page<Client> list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return repository.findByUserId(authHelper.getCurrentUserId(), pageable);
    }

    @Operation(summary = "Get client by id")
    @ApiResponse(responseCode = "200", description = "Client found")
    @ApiResponse(responseCode = "404", description = "Client not found")
    @GetMapping("/{id}")
    public Client getById(@PathVariable String id) {
        return repository.findByIdAndUserId(id, authHelper.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    @Operation(summary = "Get client ride statistics")
    @ApiResponse(responseCode = "200", description = "Client stats")
    @ApiResponse(responseCode = "404", description = "Client not found")
    @GetMapping("/{id}/stats")
    public Map<String, Object> getClientStats(@PathVariable String id) {
        if (!repository.existsByIdAndUserId(id, authHelper.getCurrentUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        var rides = rideRepository.findByClientId(id);
        long totalRides = rides.size();
        long completedRides = rides.stream().filter(r -> r.getStatus() == RideStatus.COMPLETED).count();
        BigDecimal totalSpent = rides.stream()
                .filter(r -> r.getBillableAmount() != null)
                .map(r -> r.getBillableAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant lastRide = rides.stream()
                .map(r -> r.getPickupDateTime())
                .filter(t -> t != null)
                .max(Instant::compareTo)
                .orElse(null);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRides", totalRides);
        stats.put("completedRides", completedRides);
        stats.put("totalSpent", totalSpent);
        stats.put("lastRideDate", lastRide);
        stats.put("vip", completedRides >= 5);
        return stats;
    }

    @Operation(summary = "Create a client")
    @ApiResponse(responseCode = "201", description = "Client created")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Client create(@Valid @RequestBody Client client) {
        String userId = authHelper.getCurrentUserId();
        if (repository.existsByUserIdAndPhone(userId, client.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A client with this phone number already exists");
        }
        client.setUserId(userId);
        client.setCreatedAt(Instant.now());
        Client saved = repository.save(client);
        activityLog.log(userId, "CREATE", "Client", saved.getId(),
                "Added client " + saved.getFirstName() + " " + saved.getLastName());
        return saved;
    }

    @Operation(summary = "Update a client")
    @ApiResponse(responseCode = "200", description = "Client updated")
    @ApiResponse(responseCode = "404", description = "Client not found")
    @PutMapping("/{id}")
    public Client update(@PathVariable String id, @Valid @RequestBody Client updates) {
        String userId = authHelper.getCurrentUserId();
        Client existing = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        if (!existing.getPhone().equals(updates.getPhone())
                && repository.existsByUserIdAndPhoneAndIdNot(userId, updates.getPhone(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A client with this phone number already exists");
        }
        existing.setFirstName(updates.getFirstName());
        existing.setLastName(updates.getLastName());
        existing.setPhone(updates.getPhone());
        existing.setEmail(updates.getEmail());
        existing.setNotes(updates.getNotes());
        Client saved = repository.save(existing);
        activityLog.log(userId, "UPDATE", "Client", id,
                "Updated client " + saved.getFirstName() + " " + saved.getLastName());
        return saved;
    }

    @Operation(summary = "Delete a client")
    @ApiResponse(responseCode = "204", description = "Client deleted")
    @ApiResponse(responseCode = "400", description = "Client has active rides")
    @ApiResponse(responseCode = "404", description = "Client not found")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!repository.existsByIdAndUserId(id, authHelper.getCurrentUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        var activeRides = rideRepository.findByClientIdAndStatusIn(id,
                List.of(RideStatus.SCHEDULED, RideStatus.ASSIGNED, RideStatus.IN_PROGRESS)); // ride-only check post Job refactor
        if (!activeRides.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete client with active rides (" + activeRides.size() + " active)");
        }
        repository.deleteById(id);
        activityLog.log(authHelper.getCurrentUserId(), "DELETE", "Client", id, "Deleted client");
    }
}
