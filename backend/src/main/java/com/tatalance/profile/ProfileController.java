package com.tatalance.profile;

import com.tatalance.activity.ActivityLogger;
import com.tatalance.user.AuthHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Tag(name = "Profiles", description = "Business profiles per account (owner). Multiple profiles (e.g. DRIVER, ENGINEER) per user. Jobs scoped to profile, clients shared.")
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileRepository repository;
    private final AuthHelper authHelper;
    private final ActivityLogger activityLog;

    public ProfileController(ProfileRepository repository, AuthHelper authHelper, ActivityLogger activityLog) {
        this.repository = repository;
        this.authHelper = authHelper;
        this.activityLog = activityLog;
    }

    @Operation(summary = "List all profiles for current user (account)")
    @ApiResponse(responseCode = "200", description = "Profile list")
    @GetMapping
    public Page<Profile> list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = authHelper.getCurrentUserId();
        return repository.findByUserId(userId, pageable);
    }

    @Operation(summary = "Get profile by id (must belong to current user)")
    @ApiResponse(responseCode = "200", description = "Profile found")
    @ApiResponse(responseCode = "404", description = "Profile not found")
    @GetMapping("/{id}")
    public Profile getById(@PathVariable String id) {
        String userId = authHelper.getCurrentUserId();
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    @Operation(summary = "Create a new profile for current user")
    @ApiResponse(responseCode = "201", description = "Profile created")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Profile create(@RequestBody Profile profile) {
        String userId = authHelper.getCurrentUserId();
        if (profile.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile type is required");
        }
        profile.setUserId(userId);
        profile.setCreatedAt(Instant.now());
        Profile saved = repository.save(profile);
        activityLog.log(userId, "CREATE", "Profile", saved.getId(),
                "Created " + saved.getType() + " profile: " + saved.getName());
        return saved;
    }
}
