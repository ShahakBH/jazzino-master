package com.yazino.web.form;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.io.Serializable;

public class ContactForm implements Validator, Serializable {
    private static final long serialVersionUID = 7241530510766766625L;

    private String name;
    private String email;
    private String reason;
    private String message;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    @Override
    public boolean supports(final Class clazz) {
        return ContactForm.class.equals(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(
                errors, "name", "errors.contact.name.blank", "You must enter your name");
        ValidationUtils.rejectIfEmptyOrWhitespace(
                errors, "email", "errors.contact.email.blank", "You must enter your email address");
        ValidationUtils.rejectIfEmptyOrWhitespace(
                errors, "reason", "errors.contact.reason.blank", "You must specify a contact reason");
        ValidationUtils.rejectIfEmptyOrWhitespace(
                errors, "message", "errors.contact.message.blank", "You must enter a message");
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final ContactForm rhs = (ContactForm) obj;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(email, rhs.email)
                .append(reason, rhs.reason)
                .append(message, rhs.message)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 41)
                .append(name)
                .append(email)
                .append(reason)
                .append(message)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
