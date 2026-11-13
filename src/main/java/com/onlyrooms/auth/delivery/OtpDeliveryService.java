package com.onlyrooms.auth.delivery;

import com.onlyrooms.auth.entity.OtpPurpose;

public interface OtpDeliveryService {

    void sendOtp(String destination, String otp, OtpPurpose purpose);
}