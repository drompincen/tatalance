---
title: "Issue #9 — Fix header badge: MongoDB in-memory → MongoDB Atlas"
status: completed
created: 2026-05-10
updated: 2026-05-10
current_chapter: 3
---

# Plan: Fix header badge text

## Chapter 1: RED — Write failing UI content test
**Status:** completed
**Depends on:** none

- [x] Write `HeaderBadgeTest.java` — GET /index.html asserts body contains "MongoDB Atlas", not "in-memory"
- [x] Run `mvn test` — confirm test FAILS (current text is "in-memory")

**Notes:**
> Static resources are served by @WebMvcTest — test worked without extra config.

## Chapter 2: GREEN — Fix the badge text
**Status:** completed
**Depends on:** Chapter 1

- [x] Change line 129 in `index.html`: "MongoDB in-memory" → "MongoDB Atlas"
- [x] Run `mvn test` — all 15 tests GREEN

**Notes:**
>

## Chapter 3: Done
**Status:** completed
**Depends on:** Chapter 2

- [x] Commit and push to `drom` branch
- [x] Close issue #9

**Notes:**
>
