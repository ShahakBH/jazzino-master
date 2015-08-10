package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class FriendRegistrationRequest {
    private BigDecimal playerId;
    private Set<BigDecimal> friends;

    public FriendRegistrationRequest() {
    }

    public FriendRegistrationRequest(final BigDecimal playerId,
                                     final Set<BigDecimal> friends) {
        notNull(playerId, "playerId may not be null");
        notNull(friends, "friends may not be null");

        this.playerId = playerId;
        this.friends = friends;
    }

    @SpaceId
    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public Set<BigDecimal> getFriends() {
        return friends;
    }

    public void setFriends(final Set<BigDecimal> friends) {
        this.friends = friends;
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
        final FriendRegistrationRequest rhs = (FriendRegistrationRequest) obj;
        return new EqualsBuilder()
                .append(friends, rhs.friends)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(friends)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(friends)
                .toString();
    }

}
