package com.onlyrooms.admin.controller;

import com.onlyrooms.admin.service.AdminVerificationService;
import com.onlyrooms.document.dto.DocumentReviewRequest;
import com.onlyrooms.document.dto.TenantDocumentResponse;

import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/documents")
public class AdminVerificationController {

    private final AdminVerificationService verificationService;

    public AdminVerificationController(
            AdminVerificationService verificationService) {

        this.verificationService = verificationService;
    }

    // ==========================================
    // GET PENDING DOCUMENTS
    // ==========================================

    @GetMapping("/pending")
    public ResponseEntity<List<TenantDocumentResponse>>
    getPendingDocuments() {

        return ResponseEntity.ok(
                verificationService.getPendingDocuments()
        );
    }

    // ==========================================
    // VIEW DOCUMENT
    // ==========================================

    @GetMapping("/{documentId}/file")
    public ResponseEntity<Resource> getDocumentFile(
            @PathVariable String documentId) {

        return verificationService.getDocumentFile(documentId);
    }

    // ==========================================
    // APPROVE DOCUMENT
    // ==========================================

    @PostMapping("/{documentId}/approve")
    public ResponseEntity<TenantDocumentResponse> approveDocument(

            @PathVariable String documentId,

            @AuthenticationPrincipal Jwt jwt) {

        String adminId = jwt.getSubject();

        return ResponseEntity.ok(
                verificationService.approveDocument(
                        documentId,
                        adminId
                )
        );
    }

    // ==========================================
    // REJECT DOCUMENT
    // ==========================================

    @PostMapping("/{documentId}/reject")
    public ResponseEntity<TenantDocumentResponse> rejectDocument(

            @PathVariable String documentId,

            @AuthenticationPrincipal Jwt jwt,

            @Valid @RequestBody DocumentReviewRequest request) {

        String adminId = jwt.getSubject();

        return ResponseEntity.ok(
                verificationService.rejectDocument(
                        documentId,
                        adminId,
                        request
                )
        );
    }
}