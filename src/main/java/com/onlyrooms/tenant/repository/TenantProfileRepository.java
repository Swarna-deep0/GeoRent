package com.onlyrooms.tenant.repository;

import com.onlyrooms.tenant.entity.TenantProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TenantProfileRepository
        extends MongoRepository<TenantProfile, String> {

    Optional<TenantProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);
}