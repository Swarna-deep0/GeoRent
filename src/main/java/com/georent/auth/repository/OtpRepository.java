package com.georent.auth.repository;

import com.georent.auth.entity.OtpChallenge;
import com.georent.auth.entity.OtpPurpose;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OtpRepository
        extends MongoRepository<OtpChallenge, String> {

    Optional<OtpChallenge>
    findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
            String userId,
            OtpPurpose purpose
    );

    List<OtpChallenge>
    findByUserIdAndPurposeAndConsumedFalse(
            String userId,
            OtpPurpose purpose
    );
}