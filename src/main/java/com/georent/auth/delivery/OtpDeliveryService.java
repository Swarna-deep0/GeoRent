package com.georent.auth.delivery;

import com.georent.auth.entity.OtpPurpose;

public interface OtpDeliveryService {

    void sendOtp(String destination, String otp, OtpPurpose purpose);
}