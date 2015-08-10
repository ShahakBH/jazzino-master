package com.yazino.yaps;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

/**
 * The response from submitting a {@link PushMessage}.
 */
public class PushResponse {
    public static final PushResponse OK = new PushResponse(AppleResponseCode.None, BigDecimal.ZERO);

    private final AppleResponseCode responseCode;
    private final BigDecimal playerId;

    public PushResponse(final AppleResponseCode responseCode,
                        final BigDecimal playerId) {
        this.responseCode = responseCode;
        this.playerId = playerId;
    }

    public AppleResponseCode getResponseCode() {
        return responseCode;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("responseCode", responseCode).
                append("playerId", playerId).
                toString();
    }
}
