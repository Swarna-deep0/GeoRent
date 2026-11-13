package com.onlyrooms.document.controller;

import com.onlyrooms.document.dto.TenantDocumentResponse;
import com.onlyrooms.document.entity.TenantDocumentType;
import com.onlyrooms.document.service.TenantDocumentService;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/landlord/documents")
public class LandlordDocumentController {

    private final TenantDocumentService documentService;

    public LandlordDocumentController(
            TenantDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<TenantDocumentResponse> uploadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("documentType")
            TenantDocumentType documentType,
            @RequestParam("file")
            MultipartFile file) {

        TenantDocumentResponse response =
                documentService.uploadLandlordDocument(
                        jwt.getSubject(),
                        documentType,
                        file
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TenantDocumentResponse>> getDocuments(
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                documentService.getMyLandlordDocuments(
                        jwt.getSubject()
                )
        );
    }

    @GetMapping("/{documentId}/file")
    public ResponseEntity<Resource> downloadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String documentId) {

        return documentService.getLandlordDocumentFile(
                jwt.getSubject(),
                documentId
        );
    }
}