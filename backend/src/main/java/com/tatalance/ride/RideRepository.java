package com.tatalance.ride;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Ride-specific repository extending the unified JobRepository.
 * All queries include RIDE type filter (or missing type for legacy docs) for safety during #93 refactor.
 * Collection is "jobs".
 */
public interface RideRepository extends JobRepository {

    @Query("{ 'clientId': ?0, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }")
    List<Ride> findByClientId(String clientId);

    @Query("{ 'clientId': ?0, 'status': { $in: ?1 }, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }")
    List<Ride> findByClientIdAndStatusIn(String clientId, Collection<RideStatus> statuses);

    @Query("{ 'assignedDriverId': ?0, 'status': { $in: ?1 }, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }")
    List<Ride> findByAssignedDriverIdAndStatusIn(String driverId, Collection<RideStatus> statuses);

    @Query(value = "{ 'assignedDriverId': ?0, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }", sort = "{ 'scheduledTime' : 1 }")
    List<Ride> findByAssignedDriverIdOrderByPickupDateTimeAsc(String assignedDriverId);

    @Query("{ 'userId': ?0, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }")
    List<Ride> findByUserId(String userId);

    @Query("{ 'userId': ?0, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }")
    Page<Ride> findByUserId(String userId, Pageable pageable);

    @Query("{ '_id': ?0, 'userId': ?1, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }")
    Optional<Ride> findByIdAndUserId(String id, String userId);

    @Query(value = "{ 'userId': ?0, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }", count = true)
    long countByUserId(String userId);

    @Query(value = "{ 'userId': ?0, 'status': ?1, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }", count = true)
    long countByUserIdAndStatus(String userId, RideStatus status);

    @Query(value = "{ 'userId': ?0, '$or': [ {'pickupDateTime': { $gte: ?1, $lt: ?2 }}, {'scheduledTime': { $gte: ?1, $lt: ?2 }} ], '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }", count = true)
    long countByUserIdAndPickupDateTimeBetween(String userId, java.time.Instant from, java.time.Instant to);

    @Query("{ 'userId': ?0, 'status': { $in: ?1 }, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }")
    List<Ride> findByUserIdAndStatusIn(String userId, Collection<RideStatus> statuses);

    @Query("{ 'userId': ?0, 'status': ?1, 'payoutPaid': ?2, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }")
    List<Ride> findByUserIdAndStatusAndPayoutPaid(String userId, RideStatus status, boolean payoutPaid);

    @Query("{ 'assignedDriverId': ?0, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }")
    List<Ride> findByAssignedDriverId(String assignedDriverId);

    // Profile-scoped variants for multi-profile support (profileId on Job, clients remain userId shared)
    @Query("{ 'userId': ?0, 'profileId': ?1, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }")
    Page<Ride> findByUserIdAndProfileId(String userId, String profileId, Pageable pageable);

    @Query("{ 'userId': ?0, 'profileId': ?1, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }")
    List<Ride> findByUserIdAndProfileId(String userId, String profileId);

    @Query("{ 'userId': ?0, 'profileId': ?1, 'status': { $in: ?2 }, '$or': [ { 'type': 'RIDE' }, { 'type': { $exists: false } } ] }")
    List<Ride> findByUserIdAndProfileIdAndStatusIn(String userId, String profileId, Collection<RideStatus> statuses);

    // SERVICE / Jobs profile scoped (for /jobs endpoints + profile switcher)
    @Query("{ 'userId': ?0, 'profileId': ?1, 'type': 'SERVICE' }")
    Page<Job> findJobsByUserIdAndProfileId(String userId, String profileId, Pageable pageable);

    @Query("{ 'userId': ?0, 'profileId': ?1, 'type': 'SERVICE' }")
    List<Job> findJobsByUserIdAndProfileId(String userId, String profileId);
}
