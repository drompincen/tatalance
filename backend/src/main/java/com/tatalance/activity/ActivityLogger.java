package com.tatalance.activity;

public interface ActivityLogger {
    void log(String userId, String action, String entityType, String entityId, String summary);
}
