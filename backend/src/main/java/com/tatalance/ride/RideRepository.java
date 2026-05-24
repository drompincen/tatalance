package com.tatalance.ride;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface RideRepository extends MongoRepository<Ride, String> {
    List<Ride> findByClientId(String clientId);
    List<Ride> findByClientIdAndStatusIn(String clientId, Collection<RideStatus> statuses);
}
