---
 title: "Mobile Version Testing with Flag — Two Closed Loops"
 status: in-progress
 created: 2026-06-21
 updated: 2026-06-21
 loop1: "Local test/fix/push"
 loop2: "Monitor CI + remote verify with flag"
 ---

# Mobile Flag Test Plan — Two Loops

See todo list for detailed steps in Loop 1 and Loop 2.

Use scheduler for monitoring loops.
Stop with scheduler_delete when exit criteria met.

Local test: use flag ?m=1 / ?mobile=1 + inspect with tools (open_page, curl with auth + mobile UA, grep for force-mobile, dvh, jobs complete, etc).

Remote: same on http://tatalance-drom.../?m=1

Fix with search_replace, push, re-check.

End: tell user to test on phone.
