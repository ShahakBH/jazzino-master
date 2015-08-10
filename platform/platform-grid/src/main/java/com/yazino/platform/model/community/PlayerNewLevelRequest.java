package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

@SpaceClass
public class PlayerNewLevelRequest implements Serializable {
    private static final long serialVersionUID = 7519597243223012708L;

    private String requestId;
    private BigDecimal playerId;
    private Integer level;
    private BigDecimal bonusAmount;
    private String gameType;

    //gigaspaces template
    public PlayerNewLevelRequest() {
    }

    public PlayerNewLevelRequest(final BigDecimal playerId,
                                 final Integer level,
                                 final BigDecimal bonusAmount,
                                 final String gameType) {
        this.playerId = playerId;
        this.level = level;
        this.bonusAmount = bonusAmount;
        this.gameType = gameType;
    }

    @SpaceId(autoGenerate = true)
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(final Integer level) {
        this.level = level;
    }

    public BigDecimal getBonusAmount() {
        return bonusAmount;
    }

    public void setBonusAmount(final BigDecimal bonusAmount) {
        this.bonusAmount = bonusAmount;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
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
        final PlayerNewLevelRequest rhs = (PlayerNewLevelRequest) obj;
        return new EqualsBuilder()
                .append(level, rhs.level)
                .append(bonusAmount, rhs.bonusAmount)
                .append(gameType, rhs.gameType)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(level)
                .append(bonusAmount)
                .append(gameType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(level)
                .append(bonusAmount)
                .append(gameType)
                .toString();
    }

}
