package com.onlyrooms.tenant.service;

import com.onlyrooms.tenant.dto.TenantProfileRequest;
import com.onlyrooms.tenant.dto.TenantProfileResponse;
import com.onlyrooms.tenant.entity.TenantProfile;
import com.onlyrooms.tenant.repository.TenantProfileRepository;

import com.onlyrooms.user.entity.User;
import com.onlyrooms.user.entity.UserRole;
import com.onlyrooms.user.repository.UserRepository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TenantService {

    private final TenantProfileRepository tenantRepository;
    private final UserRepository userRepository;

    public TenantService(
            TenantProfileRepository tenantRepository,
            UserRepository userRepository) {

        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    public TenantProfileResponse updateProfile(
            String userId,
            TenantProfileRequest request) {

        // 1. Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        // 2. Verify TENANT role
        if (user.getRole() != UserRole.TENANT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can update tenant profiles."
            );
        }

        // 3. Find existing profile
        TenantProfile profile = tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tenant profile not found."
                ));

        // Prevent modification of locked profiles
        if (profile.isProfileLocked()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tenant profile is locked. Verify your identity to unlock it."
            );
        }

        // 4. Normalize locations
        List<String> locations = new ArrayList<>();

        if (request.getPreferredLocations() != null) {

            for (String location : request.getPreferredLocations()) {

                if (location == null || location.isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Preferred locations cannot be empty."
                    );
                }

                String normalized = location.trim();

                if (!locations.contains(normalized)) {
                    locations.add(normalized);
                }
            }
        }

        // 5. Update permitted fields
        profile.setTenantType(request.getTenantType());

        profile.setOccupation(
                request.getOccupation() == null
                        ? null
                        : request.getOccupation().trim()
        );

        profile.setMonthlyBudget(request.getMonthlyBudget());

        profile.setPreferredLocations(locations);

        // 6. Update timestamp
        profile.setUpdatedAt(LocalDateTime.now());

        // 7. Save
        TenantProfile updated = tenantRepository.save(profile);

        return convertToResponse(updated);
    }

    public TenantProfileResponse createProfile(
            String userId,
            TenantProfileRequest request) {

        // 1. Verify user exists

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        // 2. Only TENANT can create tenant profile

        if (user.getRole() != UserRole.TENANT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can create tenant profiles."
            );
        }

        // 3. Prevent duplicate profiles

        if (tenantRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tenant profile already exists."
            );
        }

        // 4. Normalize preferred locations

        List<String> locations = new ArrayList<>();

        if (request.getPreferredLocations() != null) {

            for (String location : request.getPreferredLocations()) {

                if (location == null || location.isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Preferred locations cannot contain empty values."
                    );
                }

                String normalized = location.trim();

                if (!locations.contains(normalized)) {
                    locations.add(normalized);
                }
            }
        }

        // 5. Create profile

        TenantProfile profile = new TenantProfile(
                userId,
                request.getTenantType()
        );

        profile.setOccupation(
                request.getOccupation() == null
                        ? null
                        : request.getOccupation().trim()
        );

        profile.setMonthlyBudget(request.getMonthlyBudget());

        profile.setPreferredLocations(locations);

        profile.setUpdatedAt(LocalDateTime.now());

        // 6. Save profile

        try {

            TenantProfile saved =
                    tenantRepository.save(profile);

            return convertToResponse(saved);

        } catch (DuplicateKeyException ex) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tenant profile already exists."
            );
        }
    }

    public TenantProfileResponse lockProfile(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        if (user.getRole() != UserRole.TENANT) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can lock tenant profiles."
            );
        }

        TenantProfile profile = tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tenant profile not found."
                ));

        // Idempotent operation

        if (!profile.isProfileLocked()) {

            profile.setProfileLocked(true);
            profile.setUpdatedAt(LocalDateTime.now());

            profile = tenantRepository.save(profile);
        }

        return convertToResponse(profile);
    }

    // Unlock authenticated tenant profile

    public TenantProfileResponse unlockProfile(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        if (user.getRole() != UserRole.TENANT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can unlock tenant profiles."
            );
        }

        TenantProfile profile = tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tenant profile not found."
                ));

        if (!profile.isProfileLocked()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tenant profile is already unlocked."
            );
        }

        profile.setProfileLocked(false);
        profile.setUpdatedAt(LocalDateTime.now());

        TenantProfile updated = tenantRepository.save(profile);

        return convertToResponse(updated);
    }

    // Retrieve authenticated tenant profile

    public TenantProfileResponse getMyProfile(String userId) {

        TenantProfile profile = tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tenant profile not found."
                ));

        return convertToResponse(profile);
    }

    private TenantProfileResponse convertToResponse(
            TenantProfile profile) {

        return new TenantProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getTenantType(),
                profile.getOccupation(),
                profile.getMonthlyBudget(),
                profile.getPreferredLocations(),
                profile.isProfileLocked(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}