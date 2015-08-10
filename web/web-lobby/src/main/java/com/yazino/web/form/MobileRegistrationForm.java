package com.yazino.web.form;

import com.yazino.platform.player.PlayerProfile;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class MobileRegistrationForm implements Serializable, RegistrationForm {
    private static final long serialVersionUID = 4832026825634566545L;

    private PlayerProfile userProfile = new PlayerProfile();
    private String password;
    private String avatarURL;
    private boolean termsAndConditions;

    public boolean getTermsAndConditions() {
        return termsAndConditions;
    }

    public void setTermsAndConditions(final boolean termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }

    public String getEmail() {
        return userProfile.getEmailAddress();
    }

    public void setEmail(final String emailAddress) {
        userProfile.setEmailAddress(emailAddress);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return userProfile.getDisplayName();
    }

    public void setDisplayName(final String displayName) {
        userProfile.setDisplayName(displayName);
    }

    public PlayerProfile getUserProfile() {
        return userProfile;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    @Override
    public Boolean getOptIn() {
        return null;  //unused by web so should not be set.
    }

    public void setAvatarURL(final String avatarURL) {
        this.avatarURL = avatarURL;
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

        final MobileRegistrationForm rhs = (MobileRegistrationForm) obj;
        return new EqualsBuilder()
                .append(userProfile, rhs.userProfile)
                .append(password, rhs.password)
                .append(avatarURL, rhs.avatarURL)
                .append(termsAndConditions, rhs.termsAndConditions)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 47)
                .append(userProfile)
                .append(password)
                .append(avatarURL)
                .append(termsAndConditions)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
