package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.tournament.TournamentRegistrationInfo;
import com.yazino.platform.tournament.TournamentStatus;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class TournamentScheduleUpdateRequest {

    private String spaceId;
    private String gameType;
    private String partnerId;
    private TournamentRegistrationInfo registrationInfo;
    private TournamentStatus status;

    public TournamentScheduleUpdateRequest() {
    }

    public TournamentScheduleUpdateRequest(final String gameType,
                                           final String partnerId,
                                           final TournamentRegistrationInfo registrationInfo,
                                           final TournamentStatus status) {
        notNull(gameType, "gameType may not be null");
        notNull(partnerId, "partnerId may not be null");
        notNull(registrationInfo, "registrationInfo may not be null");
        notNull(status, "status may not be null");

        this.partnerId = partnerId;
        this.gameType = gameType;
        this.registrationInfo = registrationInfo;
        this.status = status;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    @SpaceRouting
    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(final String partnerId) {
        this.partnerId = partnerId;
    }

    public TournamentRegistrationInfo getRegistrationInfo() {
        return registrationInfo;
    }

    public void setRegistrationInfo(final TournamentRegistrationInfo registrationInfo) {
        this.registrationInfo = registrationInfo;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public void setStatus(final TournamentStatus status) {
        this.status = status;
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
        final TournamentScheduleUpdateRequest rhs = (TournamentScheduleUpdateRequest) obj;
        return new EqualsBuilder()
                .append(gameType, rhs.gameType)
                .append(partnerId, rhs.partnerId)
                .append(registrationInfo, rhs.registrationInfo)
                .append(status, rhs.status)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(gameType)
                .append(partnerId)
                .append(registrationInfo)
                .append(status)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(gameType)
                .append(partnerId)
                .append(registrationInfo)
                .append(status)
                .toString();
    }
}
