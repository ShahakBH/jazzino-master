package com.yazino.email;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notNull;

public class EmailVerificationResult implements Serializable {
    private static final long serialVersionUID = 8468124619079034987L;

    private final String address;
    private final EmailVerificationStatus status;
    private final boolean disposable;
    private final boolean role;

    public EmailVerificationResult(final String address,
                                   final EmailVerificationStatus status) {
        this(address, status, false, false);
    }

    public EmailVerificationResult(final String address,
                                   final EmailVerificationStatus status,
                                   final boolean disposable,
                                   final boolean role) {
        notNull(address, "address may not be null");
        notNull(status, "status may not be null");

        this.address = address;
        this.status = status;
        this.disposable = disposable;
        this.role = role;
    }

    public EmailVerificationStatus getStatus() {
        return status;
    }

    public boolean isDisposable() {
        return disposable;
    }

    public boolean isRole() {
        return role;
    }

    public String getAddress() {
        return address;
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
        EmailVerificationResult rhs = (EmailVerificationResult) obj;
        return new EqualsBuilder()
                .append(this.address, rhs.address)
                .append(this.status, rhs.status)
                .append(this.disposable, rhs.disposable)
                .append(this.role, rhs.role)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(address)
                .append(status)
                .append(disposable)
                .append(role)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("address", address)
                .append("status", status)
                .append("disposable", disposable)
                .append("role", role)
                .toString();
    }
}
