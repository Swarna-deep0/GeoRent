package com.onlyrooms.document.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "tenant_documents")
public class TenantDocument {

    @Id
    private String id;

    @Indexed
    private String userId;

    private TenantDocumentType documentType;

    private String originalFilename;

    private String storedFilename;

    private String contentType;

    private long fileSize;

    private DocumentVerificationStatus verificationStatus;

    private String rejectionReason;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private String reviewedBy;

    public TenantDocument() {
    }

    public TenantDocument(
            String userId,
            TenantDocumentType documentType,
            String originalFilename,
            String storedFilename,
            String contentType,
            long fileSize) {

        this.userId = userId;
        this.documentType = documentType;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;

        this.verificationStatus =
                DocumentVerificationStatus.PENDING;

        this.submittedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public TenantDocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(TenantDocumentType documentType) {
        this.documentType = documentType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public DocumentVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(
            DocumentVerificationStatus verificationStatus) {

        this.verificationStatus = verificationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}