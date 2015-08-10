package com.yazino.platform.player;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.yazino.game.api.ParameterisedMessage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerProfileUpdateResponse extends PlayerProfileServiceResponse {
    private static final long serialVersionUID = -1365037865755534135L;

    private final PlayerProfile updatedUserProfile;
    private boolean emailChanged = false;

    public PlayerProfileUpdateResponse(final PlayerProfile updatedUserProfile) {
        super(true);

        this.updatedUserProfile = updatedUserProfile;
    }

    public PlayerProfileUpdateResponse(final PlayerProfile updatedUserProfile,
                                       final ParameterisedMessage... errors) {
        this(updatedUserProfile, new HashSet<ParameterisedMessage>(Arrays.asList(errors)));
    }

    public PlayerProfileUpdateResponse(final PlayerProfile updatedUserProfile,
                                       final Set<ParameterisedMessage> errors) {
        super(errors, false);

        notNull(updatedUserProfile, "userProfile must not be null");
        this.updatedUserProfile = updatedUserProfile;
    }


    public PlayerProfile getUpdatedUserProfile() {
        return updatedUserProfile;
    }

    public boolean hasEmailChanged() {
        return emailChanged;
    }

    public void setEmailChanged(final boolean emailChanged) {
        this.emailChanged = emailChanged;
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

        final PlayerProfileUpdateResponse rhs = (PlayerProfileUpdateResponse) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(rhs))
                .append(updatedUserProfile, rhs.updatedUserProfile)
                .append(emailChanged, rhs.emailChanged)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(updatedUserProfile)
                .append(emailChanged)
                .toHashCode();
    }

}
