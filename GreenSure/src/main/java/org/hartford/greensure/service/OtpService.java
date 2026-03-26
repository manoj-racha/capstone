package org.hartford.greensure.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP store with 10-minute TTL.
 *
 * Stores a 6-digit OTP per email address. The OTP and its expiry
 * timestamp are stored together in an OtpEntry record.
 * generateAndStoreOtp() overwrites any existing entry for the same email
 * (so the user can request a resend).
 *
 * This is suitable for a capstone / development environment.
 * In production, use Redis with TTL keys.
 */
@Service
public class OtpService {

    private static final long OTP_TTL_SECONDS = 10 * 60; // 10 minutes
    private static final int  OTP_LENGTH      = 6;

    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // ── Public API ──────────────────────────────────────────────

    /** Generates a 6-digit OTP, stores it, and returns it (to be sent via email). */
    public String generateAndStoreOtp(String email) {
        String otp = buildOtp();
        store.put(email.toLowerCase(), new OtpEntry(otp, Instant.now().plusSeconds(OTP_TTL_SECONDS)));
        return otp;
    }

    /**
     * Validates the OTP for the given email.
     * Throws {@link org.hartford.greensure.exception.OtpInvalidException} if wrong.
     * Throws {@link org.hartford.greensure.exception.OtpExpiredException} if expired.
     * Removes the entry on success so it cannot be reused.
     */
    public void validateOtp(String email, String otp) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null || !entry.otp().equals(otp)) {
            throw new org.hartford.greensure.exception.OtpInvalidException(
                    "Invalid OTP. Please try again.");
        }
        if (Instant.now().isAfter(entry.expiry())) {
            store.remove(email.toLowerCase());
            throw new org.hartford.greensure.exception.OtpExpiredException(
                    "OTP has expired. Please request a new one.");
        }
        store.remove(email.toLowerCase()); // consume the OTP
    }

    /** Removes any stored OTP for the given email. */
    public void clearOtp(String email) {
        store.remove(email.toLowerCase());
    }

    // ── Private helpers ─────────────────────────────────────────

    private String buildOtp() {
        int n = 100_000 + random.nextInt(900_000);
        return String.valueOf(n);
    }

    /** Internal record — holds OTP string + its expiry instant. */
    private record OtpEntry(String otp, Instant expiry) {}
}
