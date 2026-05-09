package com.tatalance.client;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Tag(name = "Clients", description = "Client management")
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientRepository repository;

    public ClientController(ClientRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "List all clients", description = "Returns every client stored in the database")
    @ApiResponse(responseCode = "200", description = "Client list")
    @GetMapping
    public List<Client> list() {
        return repository.findAll();
    }

    @Operation(summary = "Create a client", description = "Saves a new client and returns the created document with generated id and createdAt timestamp")
    @ApiResponse(responseCode = "201", description = "Client created")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Client create(@RequestBody Client client) {
        client.setCreatedAt(Instant.now());
        return repository.save(client);
    }
}
