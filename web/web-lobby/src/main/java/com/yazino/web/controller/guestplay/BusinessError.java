package com.yazino.web.controller.guestplay;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yazino.game.api.ParameterisedMessage;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;

@JsonSerialize
class BusinessError {                           // TODO refactor with YazinoPlayerValidator - the YPV is best placed to categorise errors - e.g. use a FieldError class with a field enum...

    public final static int ERROR_CODE_INVALID_DISPLAY_NAME = 100;
    public final static int ERROR_CODE_NON_UNIQUE_EMAIL = 200;
    public final static int ERROR_CODE_GENERIC_ERROR = 300;
    public final static int ERROR_CODE_INVALID_PASSWORD = 400;
    public final static int ERROR_CODE_FACEBOOK_USER_ALREADY_HAS_AN_ACCOUNT = 500;
    public final static int ERROR_CODE_YAZINO_USER_ALREADY_HAS_AN_ACCOUNT = 501;

    @JsonProperty
    private int code;
    @JsonProperty
    private String message;

    BusinessError() {
        // for jackson
    }

    public BusinessError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        BusinessError rhs = (BusinessError) obj;
        return new EqualsBuilder()
                .append(this.code, rhs.code)
                .append(this.message, rhs.message)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(code)
                .append(message)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("code", code)
                .append("message", message)
                .toString();
    }

    public static Set<BusinessError> toBusinessErrors(Set<ParameterisedMessage> source) {
        Set<BusinessError> errors = new HashSet<>();
        for (ParameterisedMessage message : source) {
            errors.add(toBusinessError(message));
        }
        return errors;
    }

    private static BusinessError toBusinessError(ParameterisedMessage message) {
        int code = BusinessError.ERROR_CODE_GENERIC_ERROR;
        String messageText = message.toString();
        if (messageText.toLowerCase().contains("display name")) {
            code = BusinessError.ERROR_CODE_INVALID_DISPLAY_NAME;
        } else if (messageText.equals("E-mail already registered")) {
            code = BusinessError.ERROR_CODE_NON_UNIQUE_EMAIL;
        } else if (messageText.toLowerCase().contains("password")) {
            code = BusinessError.ERROR_CODE_INVALID_PASSWORD;
        } else if (messageText.equals("Facebook user already has an account")) {
            code = BusinessError.ERROR_CODE_FACEBOOK_USER_ALREADY_HAS_AN_ACCOUNT;
        } else if (messageText.equals("Cannot convert to Yazino account. E-mail already registered")) {
            code = BusinessError.ERROR_CODE_YAZINO_USER_ALREADY_HAS_AN_ACCOUNT;
        }
        return new BusinessError(code, messageText);
    }
}
