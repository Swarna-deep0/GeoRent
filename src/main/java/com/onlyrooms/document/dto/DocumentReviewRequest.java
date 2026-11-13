package com.onlyrooms.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DocumentReviewRequest {

    @NotBlank(message = "Rejection reason is required.")
    @Size(
            min = 5,
            max = 500,
            message = "Rejection reason must contain 5 to 500 characters."
    )
    private String rejectionReason;

    public DocumentReviewRequest() {
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}