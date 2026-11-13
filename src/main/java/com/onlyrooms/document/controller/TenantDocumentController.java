package com.onlyrooms.document.controller;

import com.onlyrooms.document.dto.TenantDocumentResponse;
import com.onlyrooms.document.entity.TenantDocumentType;
import com.onlyrooms.document.service.TenantDocumentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tenant/documents")
public class TenantDocumentController {

    private final TenantDocumentService documentService;

    public TenantDocumentController(
            TenantDocumentService documentService) {

        this.documentService = documentService;
    }

    // ==========================================
    // UPLOAD DOCUMENT
    // ==========================================

    @PostMapping("/upload")
    public ResponseEntity<TenantDocumentResponse> uploadDocument(

            @AuthenticationPrincipal Jwt jwt,

            @RequestParam("documentType")
            TenantDocumentType documentType,

            @RequestParam("file")
            MultipartFile file) {

        String userId = jwt.getSubject();

        TenantDocumentResponse response =
                documentService.uploadTenantDocument(
                        userId,
                        documentType,
                        file
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ==========================================
    // GET MY DOCUMENTS
    // ==========================================

    @GetMapping
    public ResponseEntity<List<TenantDocumentResponse>> getMyDocuments(

            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();

        List<TenantDocumentResponse> documents =
                documentService.getMyDocuments(userId);

        return ResponseEntity.ok(documents);
    }

    // ==========================================
    // DOWNLOAD MY DOCUMENT
    // ==========================================

    @GetMapping("/{documentId}/file")
    public ResponseEntity<org.springframework.core.io.Resource>
    downloadDocument(

            @AuthenticationPrincipal Jwt jwt,

            @PathVariable String documentId) {

        String userId = jwt.getSubject();

        return documentService.getDocumentFile(
                userId,
                documentId
        );
    }
}