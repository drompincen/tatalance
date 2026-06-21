package com.tatalance.ride;

import java.time.Instant;

/** One start→stop (or start→pause) interval for freelance time tracking. */
public class WorkSegment {
    private Instant startedAt;
    private Instant endedAt;

    public WorkSegment() {}

    public WorkSegment(Instant startedAt, Instant endedAt) {
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public boolean isOpen() { return endedAt == null; }
}