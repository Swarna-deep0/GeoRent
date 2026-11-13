package com.onlyrooms.document.service;

import com.onlyrooms.document.dto.TenantDocumentResponse;
import com.onlyrooms.document.entity.DocumentVerificationStatus;
import com.onlyrooms.document.entity.TenantDocument;
import com.onlyrooms.document.entity.TenantDocumentType;
import com.onlyrooms.document.repository.TenantDocumentRepository;
import com.onlyrooms.user.entity.User;
import com.onlyrooms.user.entity.UserRole;
import com.onlyrooms.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.MalformedURLException;

@Service
public class TenantDocumentService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final List<DocumentVerificationStatus> BLOCKING_STATUSES =
            List.of(
                    DocumentVerificationStatus.PENDING,
                    DocumentVerificationStatus.VERIFIED
            );
    private final TenantDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final Path uploadDirectory;

    public TenantDocumentService(
            TenantDocumentRepository documentRepository,
            UserRepository userRepository,
            @Value("${georent.document.upload-dir:./georent-uploads}")
            String uploadDirectory) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.uploadDirectory = Paths.get(uploadDirectory)
                .toAbsolutePath()
                .normalize();
    }

    // =====================================================
    // UPLOAD TENANT DOCUMENT
    // =====================================================
    private TenantDocumentResponse uploadDocumentForRole(
            String userId,
            TenantDocumentType documentType,
            MultipartFile file,
            UserRole requiredRole) {
        // 1. Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));
        // 2. Validate role
        if (user.getRole() != requiredRole) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to upload these documents."
            );
        }
        // 3. Validate document type
        if (documentType == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document type is required."
            );
        }
        // 4. Validate file presence
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please select a document to upload."
            );
        }
        // 5. Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Document size must not exceed 5 MB."
            );
        }
        // 6. Prevent duplicate pending or verified documents
        boolean alreadyExists =
                documentRepository
                        .existsByUserIdAndDocumentTypeAndVerificationStatusIn(
                                userId,
                                documentType,
                                BLOCKING_STATUSES
                        );
        if (alreadyExists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A pending or verified document of this type already exists."
            );
        }
        // 7. Read and validate file signature
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to read uploaded document."
            );
        }
        String extension = detectFileExtension(fileBytes);
        // 8. Validate original filename extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Original filename is required."
            );
        }
        String safeOriginalFilename = Paths.get(
                originalFilename.replace("\\", "/")
        ).getFileName().toString();
        String suppliedExtension = getExtension(safeOriginalFilename);
        if (!extension.equals(suppliedExtension)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File extension does not match the actual document format."
            );
        }
        // 9. Validate supplied MIME type
        String expectedContentType = getContentType(extension);
        String suppliedContentType = file.getContentType();
        if (suppliedContentType == null ||
                !(suppliedContentType.equalsIgnoreCase(expectedContentType)
                        || suppliedContentType.equalsIgnoreCase(
                        "application/octet-stream"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File content type does not match the document format."
            );
        }
        // 10. Generate secure UUID filename
        String storedFilename =
                UUID.randomUUID() + "." + extension;
        Path destination = uploadDirectory
                .resolve(storedFilename)
                .normalize();
        // Ensure destination remains inside upload directory
        if (!destination.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid document storage path."
            );
        }
        boolean fileStored = false;
        try {
            // 11. Create storage directory
            Files.createDirectories(uploadDirectory);
            // 12. Store document
            Files.copy(
                    new java.io.ByteArrayInputStream(fileBytes),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );
            fileStored = true;
            // 13. Create MongoDB metadata
            TenantDocument document = new TenantDocument(
                    userId,
                    documentType,
                    safeOriginalFilename,
                    storedFilename,
                    expectedContentType,
                    fileBytes.length
            );
            // Initial status is PENDING (entity constructor)
            TenantDocument saved =
                    documentRepository.save(document);
            return convertToResponse(saved);
        } catch (DataAccessException ex) {
            deleteStoredFile(destination);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to save document information."
            );
        } catch (IOException ex) {
            if (fileStored) {
                deleteStoredFile(destination);
            }
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to store uploaded document."
            );
        } catch (RuntimeException ex) {
            if (fileStored) {
                deleteStoredFile(destination);
            }
            throw ex;
        }
    }

    // =====================================================
    // FILE SIGNATURE VALIDATION
    // =====================================================
    private String detectFileExtension(byte[] bytes) {
        // PDF: %PDF-
        if (bytes.length >= 5 &&
                bytes[0] == '%' &&
                bytes[1] == 'P' &&
                bytes[2] == 'D' &&
                bytes[3] == 'F' &&
                bytes[4] == '-') {
            return "pdf";
        }
        // PNG signature
        if (bytes.length >= 8 &&
                (bytes[0] & 0xFF) == 0x89 &&
                bytes[1] == 0x50 &&
                bytes[2] == 0x4E &&
                bytes[3] == 0x47 &&
                bytes[4] == 0x0D &&
                bytes[5] == 0x0A &&
                bytes[6] == 0x1A &&
                bytes[7] == 0x0A) {
            return "png";
        }
        // JPEG signature
        if (bytes.length >= 3 &&
                (bytes[0] & 0xFF) == 0xFF &&
                (bytes[1] & 0xFF) == 0xD8 &&
                (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsupported or invalid document format. Upload PDF, JPG, JPEG or PNG."
        );
    }

    // =====================================================
    // FILE EXTENSION
    // =====================================================
    private String getExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document filename must have a valid extension."
            );
        }
        String extension = filename.substring(index + 1)
                .toLowerCase();
        if (extension.equals("jpeg")) {
            return "jpg";
        }
        return extension;
    }

    // =====================================================
    // MIME TYPE
    // =====================================================
    private String getContentType(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported document format."
            );
        };
    }

    // =====================================================
    // DELETE STORED FILE
    // =====================================================
    private void deleteStoredFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            // Do not expose filesystem details to API clients.
        }
    }

    // RESPONSE CONVERSION
    private TenantDocumentResponse convertToResponse(
            TenantDocument document) {
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

    // GET ALL DOCUMENTS OF AUTHENTICATED TENANT
    public List<TenantDocumentResponse> getMyDocuments(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        if (user.getRole() != UserRole.TENANT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can access tenant documents."
            );
        }

        return documentRepository
                .findByUserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // GET ALL DOCUMENTS OF AUTHENTICATED LANDLORD
    public List<TenantDocumentResponse> getMyLandlordDocuments(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        if (user.getRole() != UserRole.LANDLORD) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only landlords can access landlord documents."
            );
        }

        return documentRepository
                .findByUserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public TenantDocumentResponse uploadLandlordDocument(
            String userId,
            TenantDocumentType documentType,
            MultipartFile file) {
        return uploadDocumentForRole(
                userId,
                documentType,
                file,
                UserRole.LANDLORD
        );
    }

    public TenantDocumentResponse uploadTenantDocument(
            String userId,
            TenantDocumentType documentType,
            MultipartFile file) {
        return uploadDocumentForRole(
                userId,
                documentType,
                file,
                UserRole.TENANT
        );
    }

    // SECURE OWNED DOCUMENT DOWNLOAD HELPER
    private ResponseEntity<Resource> getOwnedDocumentFile(
            String userId,
            String documentId) {

        TenantDocument document = documentRepository
                .findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found."
                ));

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
            if (!Files.exists(filePath) ||
                    !Files.isRegularFile(filePath)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Stored document not found."
                );
            }

            Resource resource = new UrlResource(
                    filePath.toUri()
            );

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            document.getContentType()
                    ))
                    .contentLength(document.getFileSize())
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            org.springframework.http.ContentDisposition
                                    .attachment()
                                    .filename(document.getOriginalFilename())
                                    .build()
                                    .toString()
                    )
                    .body(resource);

        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve document."
            );
        }
    }

    // SECURE TENANT DOCUMENT DOWNLOAD
    public ResponseEntity<Resource> getDocumentFile(
            String userId,
            String documentId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        if (user.getRole() != UserRole.TENANT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can access tenant documents."
            );
        }

        return getOwnedDocumentFile(userId, documentId);
    }

    // SECURE LANDLORD DOCUMENT DOWNLOAD
    public ResponseEntity<Resource> getLandlordDocumentFile(
            String userId,
            String documentId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        if (user.getRole() != UserRole.LANDLORD) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only landlords can access these documents."
            );
        }

        return getOwnedDocumentFile(userId, documentId);
    }
}