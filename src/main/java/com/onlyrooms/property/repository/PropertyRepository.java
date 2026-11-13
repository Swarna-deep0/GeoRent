package com.onlyrooms.property.repository;

import com.onlyrooms.property.entity.Property;
import com.onlyrooms.property.entity.PropertyVerificationStatus;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends MongoRepository<Property, String> {

    // Retrieve all properties owned by a landlord.
    List<Property> findByLandlordIdOrderByCreatedAtDesc(String landlordId);

    // Find a specific property belonging to a particular landlord.
    Optional<Property> findByIdAndLandlordId(String id, String landlordId);

    // Retrieve properties awaiting admin review or having another verification status.
    List<Property> findByVerificationStatusOrderByCreatedAtAsc(
            PropertyVerificationStatus verificationStatus
    );
}