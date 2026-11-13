package com.onlyrooms.document.repository;

import com.onlyrooms.document.entity.TenantDocument;
import com.onlyrooms.document.entity.TenantDocumentType;
import com.onlyrooms.document.entity.DocumentVerificationStatus;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TenantDocumentRepository
        extends MongoRepository<TenantDocument, String> {

    // Find all documents submitted by a particular tenant
    List<TenantDocument> findByUserIdOrderBySubmittedAtDesc(
            String userId
    );

    // Find documents by verification status
    List<TenantDocument> findByVerificationStatusOrderBySubmittedAtAsc(
            DocumentVerificationStatus status
    );

    // Find a particular document belonging to a tenant
    Optional<TenantDocument> findByIdAndUserId(
            String id,
            String userId
    );

    // Check whether a pending or verified document already exists
    boolean existsByUserIdAndDocumentTypeAndVerificationStatusIn(
            String userId,
            TenantDocumentType documentType,
            List<DocumentVerificationStatus> statuses
    );

    // Find the latest document of a particular type
    Optional<TenantDocument>
    findFirstByUserIdAndDocumentTypeOrderBySubmittedAtDesc(
            String userId,
            TenantDocumentType documentType
    );
}