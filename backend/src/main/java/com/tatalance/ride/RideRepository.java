package com.tatalance.ride;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RideRepository extends MongoRepository<Ride, String> {
    List<Ride> findByClientId(String clientId);
    List<Ride> findByClientIdAndStatusIn(String clientId, Collection<RideStatus> statuses);
    List<Ride> findByAssignedDriverIdAndStatusIn(String driverId, Collection<RideStatus> statuses);
    List<Ride> findByAssignedDriverIdOrderByPickupDateTimeAsc(String assignedDriverId);
    List<Ride> findByUserId(String userId);
    Optional<Ride> findByIdAndUserId(String id, String userId);
}
