package com.yazino.platform.player;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Set;

public class PlayerInformationHolder implements Serializable {
    private static final long serialVersionUID = 7241530510766766625L;

    private PlayerProfile playerProfile;
    private String sessionKey;
    private Set<String> friends;
    private ReferralRecipient referralRecipient;
    private String avatarUrl;

    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }

    public void setPlayerProfile(final PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(final String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Set<String> getFriends() {
        return friends;
    }

    public void setFriends(final Set<String> friends) {
        this.friends = friends;
    }

    public ReferralRecipient getReferralRecipient() {
        return referralRecipient;
    }

    public void setReferralRecipient(final ReferralRecipient referralRecipient) {
        this.referralRecipient = referralRecipient;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(final String avatarUrl) {
        this.avatarUrl = avatarUrl;
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
        final PlayerInformationHolder rhs = (PlayerInformationHolder) obj;
        return new EqualsBuilder()
                .append(playerProfile, rhs.playerProfile)
                .append(referralRecipient, rhs.referralRecipient)
                .append(friends, rhs.friends)
                .append(sessionKey, rhs.sessionKey)
                .append(avatarUrl, rhs.avatarUrl)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(playerProfile)
                .append(referralRecipient)
                .append(friends)
                .append(sessionKey)
                .append(avatarUrl)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerProfile)
                .append(referralRecipient)
                .append(friends)
                .append(sessionKey)
                .append(avatarUrl)
                .toString();
    }

}
