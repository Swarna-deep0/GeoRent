package com.onlyrooms.document.dto;

import com.onlyrooms.document.entity.TenantDocumentType;
import com.onlyrooms.document.entity.DocumentVerificationStatus;

import java.time.LocalDateTime;

public class TenantDocumentResponse {

    private String id;

    private String userId;

    private TenantDocumentType documentType;

    private String originalFilename;

    private String contentType;

    private long fileSize;

    private DocumentVerificationStatus verificationStatus;

    private String rejectionReason;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private String reviewedBy;

    public TenantDocumentResponse() {
    }

    public TenantDocumentResponse(
            String id,
            String userId,
            TenantDocumentType documentType,
            String originalFilename,
            String contentType,
            long fileSize,
            DocumentVerificationStatus verificationStatus,
            String rejectionReason,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            String reviewedBy) {

        this.id = id;
        this.userId = userId;
        this.documentType = documentType;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.verificationStatus = verificationStatus;
        this.rejectionReason = rejectionReason;
        this.submittedAt = submittedAt;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
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