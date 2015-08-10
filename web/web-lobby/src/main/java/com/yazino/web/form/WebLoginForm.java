package com.yazino.web.form;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class WebLoginForm extends LoginForm implements RegistrationForm {
    private static final long serialVersionUID = 7241530510766766625L;

    private String displayName;
    private String redirectTo;
    private String honeypot;
    private String registered;
    private String avatar;
    private boolean termsAndConditions;
    private String registeredPassword;
    private Boolean optIn;

    public void setAvatar(final String avatar) {
        this.avatar = avatar;
    }

    public WebLoginForm() {
    }

    public void setRegistered(final String registered) {
        this.registered = registered;
    }

    public String getRegistered() {
        return registered;
    }

    public String getRedirectTo() {
        return redirectTo;
    }

    public void setRedirectTo(final String redirectTo) {
        this.redirectTo = redirectTo;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getAvatarURL() {
        return avatar;
    }

    @Override
    public Boolean getOptIn() {
        return optIn;
    }

    public void setOptIn(final Boolean optIn) {
        this.optIn = optIn;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
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

        final WebLoginForm rhs = (WebLoginForm) obj;
        return new EqualsBuilder()
                .append(getEmail(), rhs.getEmail())
                .append(getPassword(), rhs.getPassword())
                .append(get_spring_security_remember_me(), rhs.get_spring_security_remember_me())
                .append(displayName, rhs.displayName)
                .append(redirectTo, rhs.redirectTo)
                .append(honeypot, rhs.honeypot)
                .append(registered, rhs.registered)
                .append(avatar, rhs.avatar)
                .append(termsAndConditions, rhs.termsAndConditions)
                .append(registeredPassword, rhs.registeredPassword)
                .isEquals();
    }

    public boolean getTermsAndConditions() {
        return termsAndConditions;
    }

    public void setTermsAndConditions(final boolean termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getEmail())
                .append(getPassword())
                .append(get_spring_security_remember_me())
                .append(displayName)
                .append(redirectTo)
                .append(honeypot)
                .append(registered)
                .append(avatar)
                .append(termsAndConditions)
                .append(registeredPassword)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(getEmail())
                .append(getPassword())
                .append(get_spring_security_remember_me())
                .append(displayName)
                .append(redirectTo)
                .append(honeypot)
                .append(registered)
                .append(avatar)
                .append(termsAndConditions)
                .append(registeredPassword)
                .toString();
    }

    public String getHoneypot() {
        return honeypot;
    }

    public void setHoneypot(final String honeypot) {
        this.honeypot = honeypot;
    }

    public String getRegisteredPassword() {
        return registeredPassword;
    }

    public void setRegisteredPassword(final String registeredPassword) {
        this.registeredPassword = registeredPassword;
    }

}
