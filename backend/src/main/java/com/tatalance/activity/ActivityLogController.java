package com.tatalance.activity;

import com.tatalance.user.AuthHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
public class ActivityLogController {

    private final ActivityLogRepository repository;
    private final AuthHelper authHelper;

    public ActivityLogController(ActivityLogRepository repository, AuthHelper authHelper) {
        this.repository = repository;
        this.authHelper = authHelper;
    }

    @GetMapping
    public Page<ActivityLog> list(
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return repository.findByUserId(authHelper.getCurrentUserId(), pageable);
    }
}
