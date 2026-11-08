package com.georent.auth.delivery;

import com.georent.auth.entity.OtpPurpose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod")
public class ConsoleOtpDeliveryService implements OtpDeliveryService {

    private static final Logger logger =
            LoggerFactory.getLogger(ConsoleOtpDeliveryService.class);

    @Override
    public void sendOtp(
            String destination,
            String otp,
            OtpPurpose purpose) {

        logger.warn(
                "DEVELOPMENT OTP | Purpose: {} | Destination: {} | OTP: {}",
                purpose,
                destination,
                otp
        );
    }
}