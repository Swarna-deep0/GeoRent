package com.onlyrooms.landlord.repository;

import com.onlyrooms.landlord.entity.LandlordProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LandlordProfileRepository
        extends MongoRepository<LandlordProfile, String> {

    Optional<LandlordProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);
}