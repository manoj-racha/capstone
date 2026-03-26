package org.hartford.greensure.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Configuration class that creates a Gmail API client bean.
 *
 * Why a separate @Configuration class:
 *   - Separates infrastructure wiring from business logic.
 *   - The Gmail client is expensive to build (TLS handshake, credential
 *     validation) — creating it once as a singleton bean and reusing it
 *     across all email sends avoids repeated setup costs.
 *   - If credentials are wrong, the application fails at startup with a
 *     clear error message instead of silently failing on the first email.
 *
 * What the Gmail bean does:
 *   - It is the main client for making Gmail REST API calls.
 *   - Internally it holds a UserCredentials object that automatically
 *     refreshes the OAuth2 access token (1 hour lifetime) before each
 *     API call using the long-lived refresh token.
 *
 * What happens if credentials are wrong:
 *   - The application will fail to start with a RuntimeException
 *     containing a clear message. This is intentional — better to
 *     fail fast than to start successfully and silently not send emails.
 *
 * Why GoogleNetHttpTransport.newTrustedTransport():
 *   - Sets up a proper TLS-enabled HTTP transport using the JVM's
 *     trusted certificate store — required for all HTTPS calls to Google.
 *
 * Why GsonFactory instead of JacksonFactory:
 *   - Google's API client libraries bundle Gson internally.
 *     Using GsonFactory avoids potential Jackson version conflicts
 *     with Spring Boot's own Jackson dependency.
 */
@Configuration
public class GmailApiConfig {

    private static final Logger log = LoggerFactory.getLogger(GmailApiConfig.class);

    @Value("${gmail.client.id}")
    private String clientId;

    @Value("${gmail.client.secret}")
    private String clientSecret;

    @Value("${gmail.refresh.token}")
    private String refreshToken;

    @Bean
    public Gmail gmail() {
        try {
            // Build OAuth2 credentials using the refresh token.
            // UserCredentials automatically exchanges the refresh token
            // for a short-lived access token before each API call.
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .build();

            // Adapt Google auth credentials to the HTTP request pipeline
            HttpCredentialsAdapter requestInitializer =
                    new HttpCredentialsAdapter(credentials);

            // Build the Gmail service client
            Gmail gmail = new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    requestInitializer)
                    .setApplicationName("GreenSure")
                    .build();

            log.info("Gmail API client initialised successfully");
            return gmail;

        } catch (GeneralSecurityException | IOException e) {
            // Fail-fast: if the Gmail client cannot be created, the
            // application must not start. This prevents a scenario
            // where the app runs but silently cannot send any email.
            throw new RuntimeException(
                    "Failed to initialise Gmail API client — check credentials "
                            + "in application.properties: " + e.getMessage(), e);
        }
    }
}
