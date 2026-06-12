package com.tatalance.client;

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

@Tag(name = "Clients", description = "Client management")
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientRepository repository;
    private final RideRepository rideRepository;
    private final AuthHelper authHelper;

    public ClientController(ClientRepository repository, RideRepository rideRepository, AuthHelper authHelper) {
        this.repository = repository;
        this.rideRepository = rideRepository;
        this.authHelper = authHelper;
    }

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
        return repository.save(client);
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
        return repository.save(existing);
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
                List.of(RideStatus.SCHEDULED, RideStatus.ASSIGNED, RideStatus.IN_PROGRESS));
        if (!activeRides.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete client with active rides (" + activeRides.size() + " active)");
        }
        repository.deleteById(id);
    }
}
