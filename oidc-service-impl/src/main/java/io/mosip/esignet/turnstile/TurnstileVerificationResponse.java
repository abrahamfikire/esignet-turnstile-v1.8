/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package io.mosip.esignet.turnstile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Maps the JSON response from Cloudflare Turnstile's siteverify endpoint:
 * https://challenges.cloudflare.com/turnstile/v0/siteverify
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TurnstileVerificationResponse {

    /** true if the token is valid and the challenge was solved. */
    private boolean success;

    /** ISO-8601 timestamp of the challenge. */
    @JsonProperty("challenge_ts")
    private String challengeTs;

    /** Hostname where the challenge was solved. */
    private String hostname;

    /** List of error codes returned when success=false. */
    @JsonProperty("error-codes")
    private List<String> errorCodes;

    /** Action label set in the widget (optional). */
    private String action;

    /** Customer data passed during widget render (optional). */
    private String cdata;
}
