package com.yazino.platform.player.util;

import com.yazino.platform.player.PlayerProfile;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This class will merge 2 {@link com.yazino.platform.player.PlayerProfile}s
 * and report as to whether certain fields have changed.
 * <p/>
 * Note only the following fields are merged: emailAddress, country,
 * displayName, avatarURL, realName and gender.
 */
public class PlayerProfileMerger {

    private final PlayerProfile baseProfile;
    private PlayerProfile merged = new PlayerProfile();

    public PlayerProfileMerger(final PlayerProfile baseProfile) {
        notNull(baseProfile, "baseProfile must not be null");
        this.baseProfile = baseProfile;
        reset();
    }

    public PlayerProfile merge(final PlayerProfile profile) {
        notNull(profile, "profile must not be null");
        if (!StringUtils.isBlank(profile.getEmailAddress())) {
            merged.setEmailAddress(profile.getEmailAddress());
        }
        if (!StringUtils.isBlank(profile.getCountry())) {
            merged.setCountry(profile.getCountry());
        }
        if (!StringUtils.isBlank(profile.getDisplayName())) {
            merged.setDisplayName(profile.getDisplayName());
        }
        if (!StringUtils.isBlank(profile.getRealName())) {
            merged.setRealName(profile.getRealName());
        }
        if (profile.getGender() != null) {
            merged.setGender(profile.getGender());
        }
        if (profile.getDateOfBirth() != null) {
            merged.setDateOfBirth(profile.getDateOfBirth());
        }
        if (profile.getGuestStatus() != null) {
            merged.setGuestStatus(profile.getGuestStatus());
        }
        merged.setSyncProfile(profile.isSyncProfile());
        return merged;
    }

    public void reset() {
        merged = PlayerProfile.copy(baseProfile).asProfile();
    }

    public boolean hasEmailAddressChanged() {
        return !ObjectUtils.equals(baseProfile.getEmailAddress(), merged.getEmailAddress());
    }

    public boolean hasDisplayNameChanged() {
        return !ObjectUtils.equals(baseProfile.getDisplayName(), merged.getDisplayName());
    }

    public boolean hasRealNameChanged() {
        return !ObjectUtils.equals(baseProfile.getRealName(), merged.getRealName());
    }

    public boolean hasGenderChanged() {
        return !ObjectUtils.equals(baseProfile.getGender(), merged.getGender());
    }

    public boolean hasGuestStatusChanged() {
        return !ObjectUtils.equals(baseProfile.getGuestStatus(), merged.getGuestStatus());
    }

    public boolean hasCountryChanged() {
        return !ObjectUtils.equals(baseProfile.getCountry(), merged.getCountry());
    }

    public boolean hasDateOfBirthChanged() {
        return !ObjectUtils.equals(baseProfile.getDateOfBirth(), merged.getDateOfBirth());
    }

    public boolean haveFieldsChanged() {
        return hasCountryChanged()
                || hasDisplayNameChanged()
                || hasEmailAddressChanged()
                || hasGenderChanged()
                || hasGuestStatusChanged()
                || hasRealNameChanged()
                || hasDateOfBirthChanged();
    }
}
