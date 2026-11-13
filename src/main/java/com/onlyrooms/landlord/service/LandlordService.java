package com.onlyrooms.landlord.service;

import com.onlyrooms.landlord.dto.LandlordProfileRequest;
import com.onlyrooms.landlord.dto.LandlordProfileResponse;
import com.onlyrooms.landlord.entity.LandlordProfile;
import com.onlyrooms.landlord.repository.LandlordProfileRepository;
import com.onlyrooms.user.entity.User;
import com.onlyrooms.user.entity.UserRole;
import com.onlyrooms.user.repository.UserRepository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class LandlordService {

    private final LandlordProfileRepository landlordRepository;
    private final UserRepository userRepository;

    public LandlordService(
            LandlordProfileRepository landlordRepository,
            UserRepository userRepository) {

        this.landlordRepository = landlordRepository;
        this.userRepository = userRepository;
    }

    public LandlordProfileResponse createProfile(
            String userId,
            LandlordProfileRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != UserRole.LANDLORD) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only landlords can create landlord profiles");
        }

        if (landlordRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Landlord profile already exists");
        }

        LandlordProfile profile =
                new LandlordProfile(
                        userId,
                        request.getLandlordType().trim());

        applyRequest(profile, request);

        try {
            return new LandlordProfileResponse(
                    landlordRepository.save(profile));
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Landlord profile already exists");
        }
    }

    public LandlordProfileResponse getProfile(String userId) {

        LandlordProfile profile = landlordRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Landlord profile not found"));

        return new LandlordProfileResponse(profile);
    }

    public LandlordProfileResponse updateProfile(
            String userId,
            LandlordProfileRequest request) {

        LandlordProfile profile = landlordRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Landlord profile not found"));

        if (profile.isProfileLocked()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Profile is locked. Unlock it using OTP first.");
        }

        applyRequest(profile, request);

        profile.setUpdatedAt(LocalDateTime.now());

        return new LandlordProfileResponse(
                landlordRepository.save(profile));
    }

    private void applyRequest(
            LandlordProfile profile,
            LandlordProfileRequest request) {

        profile.setLandlordType(request.getLandlordType().trim());
        profile.setAddress(request.getAddress().trim());
        profile.setCity(request.getCity().trim());
        profile.setState(request.getState().trim());
        profile.setPostalCode(request.getPostalCode().trim());
    }
}