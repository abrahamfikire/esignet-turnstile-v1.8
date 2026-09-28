/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package io.mosip.esignet.turnstile;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Verifies Cloudflare Turnstile tokens server-side by calling:
 *   POST https://challenges.cloudflare.com/turnstile/v0/siteverify
 *
 * Configuration properties:
 *   mosip.esignet.turnstile.secret-key  – your Cloudflare secret key (required when provider=turnstile)
 *   mosip.esignet.turnstile.verify-url  – override the siteverify URL (optional, for testing)
 *   mosip.esignet.captcha.provider      – must equal "turnstile" to activate this service
 */
@Slf4j
@Service
public class TurnstileVerificationService {

    private static final String DEFAULT_VERIFY_URL =
            "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    /** Maximum token length accepted — prevents oversized payload attacks. */
    private static final int MAX_TOKEN_LENGTH = 4096;

    /** HTTP connect/read timeout in milliseconds. */
    private static final int TIMEOUT_MS = 5_000;

    @Value("${mosip.esignet.captcha.provider:legacy}")
    private String captchaProvider;

    @Value("${mosip.esignet.turnstile.secret-key:}")
    private String secretKey;

    @Value("${mosip.esignet.turnstile.verify-url:" + DEFAULT_VERIFY_URL + "}")
    private String verifyUrl;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Returns true if the Turnstile token is valid.
     * Returns false (without throwing) when the provider is not "turnstile",
     * the token is blank/too long, or Cloudflare reports failure.
     */
    public boolean verifyToken(String token) {
        if (!"turnstile".equalsIgnoreCase(captchaProvider)) {
            return false;
        }

        if (!StringUtils.hasText(token) || token.length() > MAX_TOKEN_LENGTH) {
            log.warn("Turnstile token is blank or exceeds maximum length");
            return false;
        }

        if (!StringUtils.hasText(secretKey)) {
            log.error("mosip.esignet.turnstile.secret-key is not configured");
            return false;
        }

        try {
            String body = "secret=" + URLEncoder.encode(secretKey, StandardCharsets.UTF_8)
                    + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

            URL url = new URL(verifyUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                log.error("Turnstile siteverify returned HTTP {}", status);
                return false;
            }

            TurnstileVerificationResponse response =
                    objectMapper.readValue(conn.getInputStream(), TurnstileVerificationResponse.class);

            if (!response.isSuccess()) {
                log.warn("Turnstile verification failed: {}", response.getErrorCodes());
            }

            return response.isSuccess();

        } catch (Exception ex) {
            log.error("Failed to call Turnstile siteverify endpoint", ex);
            return false;
        }
    }
}
