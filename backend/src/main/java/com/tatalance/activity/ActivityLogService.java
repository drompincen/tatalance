package com.tatalance.activity;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ActivityLogService implements ActivityLogger {

    private final ActivityLogRepository repository;

    public ActivityLogService(ActivityLogRepository repository) {
        this.repository = repository;
    }

    public void log(String userId, String action, String entityType, String entityId, String summary) {
        ActivityLog entry = new ActivityLog();
        entry.setUserId(userId);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setSummary(summary);
        entry.setTimestamp(Instant.now());
        repository.save(entry);
    }
}
