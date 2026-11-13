package com.onlyrooms.admin.service;

import com.onlyrooms.document.dto.DocumentReviewRequest;
import com.onlyrooms.document.dto.TenantDocumentResponse;
import com.onlyrooms.document.entity.DocumentVerificationStatus;
import com.onlyrooms.document.entity.TenantDocument;
import com.onlyrooms.document.entity.TenantDocumentType;
import com.onlyrooms.document.repository.TenantDocumentRepository;
import com.onlyrooms.landlord.entity.LandlordProfile;
import com.onlyrooms.landlord.entity.LandlordVerificationStatus;
import com.onlyrooms.landlord.repository.LandlordProfileRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AdminVerificationService {

    private final TenantDocumentRepository documentRepository;
    private final LandlordProfileRepository landlordProfileRepository;
    private final Path uploadDirectory;

    public AdminVerificationService(
            TenantDocumentRepository documentRepository,
            LandlordProfileRepository landlordProfileRepository,
            @Value("${georent.document.upload-dir:./georent-uploads}")
            String uploadDirectory) {

        this.documentRepository = documentRepository;
        this.landlordProfileRepository = landlordProfileRepository;
        this.uploadDirectory = Paths.get(uploadDirectory)
                .toAbsolutePath()
                .normalize();
    }

    // ==========================================
    // GET PENDING DOCUMENTS
    // ==========================================

    public List<TenantDocumentResponse> getPendingDocuments() {
        return documentRepository
                .findByVerificationStatusOrderBySubmittedAtAsc(
                        DocumentVerificationStatus.PENDING
                )
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // ==========================================
    // APPROVE DOCUMENT
    // ==========================================

    public TenantDocumentResponse approveDocument(
            String documentId,
            String adminId) {

        TenantDocument document = findDocument(documentId);
        validatePending(document);

        document.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
        document.setRejectionReason(null);
        document.setReviewedAt(LocalDateTime.now());
        document.setReviewedBy(adminId);

        TenantDocument saved = documentRepository.save(document);

        synchronizeLandlordVerification(saved.getUserId());

        return convertToResponse(saved);
    }

    // ==========================================
    // REJECT DOCUMENT
    // ==========================================

    public TenantDocumentResponse rejectDocument(
            String documentId,
            String adminId,
            DocumentReviewRequest request) {

        TenantDocument document = findDocument(documentId);
        validatePending(document);

        if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rejection reason is required."
            );
        }

        String reason = request.getRejectionReason().trim();
        if (reason.length() < 5 || reason.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rejection reason must contain 5 to 500 characters."
            );
        }

        document.setVerificationStatus(DocumentVerificationStatus.REJECTED);
        document.setRejectionReason(reason);
        document.setReviewedAt(LocalDateTime.now());
        document.setReviewedBy(adminId);

        TenantDocument saved = documentRepository.save(document);

        synchronizeLandlordVerification(saved.getUserId());

        return convertToResponse(saved);
    }

    // ==========================================
    // ADMIN DOCUMENT VIEW
    // ==========================================

    public ResponseEntity<Resource> getDocumentFile(String documentId) {
        TenantDocument document = findDocument(documentId);

        Path filePath = uploadDirectory
                .resolve(document.getStoredFilename())
                .normalize();

        if (!filePath.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid document path."
            );
        }

        try {
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Stored document not found."
                );
            }

            Resource resource = new UrlResource(filePath.toUri());
            MediaType mediaType = MediaType.parseMediaType(document.getContentType());

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(document.getFileSize())
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline()
                                    .filename(document.getOriginalFilename())
                                    .build()
                                    .toString()
                    )
                    .body(resource);

        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve document."
            );
        }
    }

    // ==========================================
    // LANDLORD VERIFICATION SYNCHRONIZATION
    // ==========================================

    private void synchronizeLandlordVerification(String userId) {
        LandlordProfile profile = landlordProfileRepository
                .findByUserId(userId)
                .orElse(null);

        // Document may belong to a tenant rather than a landlord
        if (profile == null) {
            return;
        }

        List<TenantDocument> documents = documentRepository
                .findByUserIdOrderBySubmittedAtDesc(userId);

        TenantDocument identityDocument = documents.stream()
                .filter(d -> d.getDocumentType() == TenantDocumentType.IDENTITY)
                .max(Comparator.comparing(TenantDocument::getSubmittedAt))
                .orElse(null);

        TenantDocument addressDocument = documents.stream()
                .filter(d -> d.getDocumentType() == TenantDocumentType.ADDRESS_PROOF)
                .max(Comparator.comparing(TenantDocument::getSubmittedAt))
                .orElse(null);

        if (identityDocument == null || addressDocument == null) {
            profile.setVerificationStatus(LandlordVerificationStatus.PENDING);
        } else if (identityDocument.getVerificationStatus() == DocumentVerificationStatus.REJECTED
                || addressDocument.getVerificationStatus() == DocumentVerificationStatus.REJECTED) {
            profile.setVerificationStatus(LandlordVerificationStatus.REJECTED);
        } else if (identityDocument.getVerificationStatus() == DocumentVerificationStatus.VERIFIED
                && addressDocument.getVerificationStatus() == DocumentVerificationStatus.VERIFIED) {
            profile.setVerificationStatus(LandlordVerificationStatus.VERIFIED);
        } else {
            profile.setVerificationStatus(LandlordVerificationStatus.PENDING);
        }

        profile.setUpdatedAt(LocalDateTime.now());
        landlordProfileRepository.save(profile);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private TenantDocument findDocument(String documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found."
                ));
    }

    private void validatePending(TenantDocument document) {
        if (document.getVerificationStatus() != DocumentVerificationStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending documents can be reviewed."
            );
        }
    }

    private TenantDocumentResponse convertToResponse(TenantDocument document) {
        return new TenantDocumentResponse(
                document.getId(),
                document.getUserId(),
                document.getDocumentType(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getFileSize(),
                document.getVerificationStatus(),
                document.getRejectionReason(),
                document.getSubmittedAt(),
                document.getReviewedAt(),
                document.getReviewedBy()
        );
    }
}