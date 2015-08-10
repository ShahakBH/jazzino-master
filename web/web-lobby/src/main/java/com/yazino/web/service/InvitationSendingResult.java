package com.yazino.web.service;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class InvitationSendingResult {

    public enum ResultCode { INVALID_ADDRESS, ALREADY_REGISTERED, LIMIT_EXCEEDED }

    private final int successful;
    private final Set<Rejection> rejections;

    public InvitationSendingResult(final int successful, final Set<Rejection> rejections) {
        this.successful = successful;
        this.rejections = Collections.unmodifiableSet(new HashSet<Rejection>(rejections));
    }

    public int getSuccessful() {
        return successful;
    }

    public Set<Rejection> getRejections() {
        return rejections;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
         return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final InvitationSendingResult rhs = (InvitationSendingResult) obj;
        return new EqualsBuilder()
                .append(successful, rhs.successful)
                .append(rejections, rhs.rejections)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(successful)
                .append(rejections)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }

    public static class Rejection {

        private final String email;
        private final ResultCode resultCode;

        public Rejection(final String email, final ResultCode resultCode) {
            this.email = email;
            this.resultCode = resultCode;
        }

        public String getEmail() {
            return email;
        }

        public ResultCode getResultCode() {
            return resultCode;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
             return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            final Rejection rhs = (Rejection) obj;
            return new EqualsBuilder()
                    .append(email, rhs.email)
                    .append(resultCode, rhs.resultCode)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(13, 17)
                    .append(email)
                    .append(resultCode)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ReflectionToStringBuilder(this).toString();
        }
    }
}
