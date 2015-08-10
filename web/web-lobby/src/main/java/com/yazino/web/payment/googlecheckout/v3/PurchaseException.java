package com.yazino.web.payment.googlecheckout.v3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yazino.web.payment.PurchaseStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIgnoreProperties({"cause", "message", "localizedMessage", "stackTrace", "suppressed", "debugMessage"})
public class PurchaseException extends Exception {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseException.class);
    private static final int MAX_ERROR_MESSAGE_LENGTH = 255;

    private PurchaseStatus status;
    @JsonProperty
    private boolean canConsume;
    private String errorMessage;
    private String debugMessage;

    @JsonCreator
    public static PurchaseException forValue(@JsonProperty("status") String status,
                                             @JsonProperty("canConsume") boolean canConsume,
                                             @JsonProperty("errorMessage") String errorMessage) {
        return new PurchaseException(PurchaseStatus.valueOf(status), canConsume, errorMessage);
    }

    public PurchaseException(PurchaseStatus status, boolean canConsume, String errorMessage) {
        this(status, canConsume, errorMessage, null, null);
    }

    public PurchaseException(PurchaseStatus status, boolean canConsume, String errorMessage, String debugMessage) {
        this(status, canConsume, errorMessage, debugMessage, null);
    }

    public PurchaseException(PurchaseStatus status, boolean canConsume, String errorMessage, Throwable cause) {
        this(status, canConsume, errorMessage, null, cause);


    }

    public PurchaseException(PurchaseStatus status, boolean canConsume, String errorMessage, String debugMessage, Throwable cause) {
        super(errorMessage, cause);
        this.status = status;
        this.canConsume = canConsume;
        this.errorMessage = errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH ? errorMessage : errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
        this.debugMessage = debugMessage;
        if (errorMessage.length() > MAX_ERROR_MESSAGE_LENGTH) {
            LOG.warn("Error message truncated to 255 characters. Was \"{}\"", errorMessage);
        }
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDebugMessage() {
        return debugMessage;
    }

    public void setDebugMessage(String debugMessage) {
        this.debugMessage = debugMessage;
    }

    // True if the client should consume the order from the inventory.  This should only be true if the server
    // has successfully recorded the changes dictated by the order.
    public boolean canConsume() {
        return canConsume;
    }
}
