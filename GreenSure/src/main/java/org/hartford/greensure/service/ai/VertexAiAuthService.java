package org.hartford.greensure.service.ai;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * OAuth2 access tokens for Vertex AI REST calls using
 * <strong>Application Default Credentials</strong> (ADC).
 * <p>
 * Local setup: run {@code gcloud auth application-default login} with a user or
 * service account that has access to the Vertex AI project. No JSON key file is
 * required when ADC is configured.
 */
@Service
@Slf4j
public class VertexAiAuthService {

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    /**
     * @return Bearer token, or empty if ADC is missing or refresh fails
     */
    public synchronized Optional<String> getAccessToken() {
        try {
            if (cachedToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(60))) {
                return Optional.of(cachedToken);
            }

            GoogleCredentials credentials =
                    GoogleCredentials.getApplicationDefault()
                            .createScoped("https://www.googleapis.com/auth/cloud-platform");
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            if (token == null) {
                log.warn("Vertex AI: GoogleCredentials returned no access token (check ADC).");
                return Optional.empty();
            }
            cachedToken = token.getTokenValue();
            if (token.getExpirationTime() != null) {
                tokenExpiry = token.getExpirationTime().toInstant();
            } else {
                tokenExpiry = Instant.now().plusSeconds(3500);
            }
            log.info("Vertex AI access token refreshed (ADC), expires: {}", tokenExpiry);
            return Optional.of(cachedToken);
        } catch (IOException e) {
            log.error("Vertex AI ADC authentication failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
