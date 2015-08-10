package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class UpdatePlayerRequest implements Serializable {
    private static final long serialVersionUID = 12312L;

    private String spaceId;
    private BigDecimal playerId;
    private String displayName;
    private String pictureLocation;
    private PaymentPreferences paymentPreferences;

    public UpdatePlayerRequest() {
    }

    public UpdatePlayerRequest(final BigDecimal playerId,
                               final String displayName,
                               final String pictureLocation,
                               final PaymentPreferences paymentPreferences) {
        notNull(playerId, "Player ID may not be null");

        this.playerId = playerId;
        this.displayName = displayName;
        this.pictureLocation = pictureLocation;
        this.paymentPreferences = paymentPreferences;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public String getPictureLocation() {
        return pictureLocation;
    }

    public void setPictureLocation(final String pictureLocation) {
        this.pictureLocation = pictureLocation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public PaymentPreferences getPaymentPreferences() {
        return paymentPreferences;
    }

    public void setPaymentPreferences(final PaymentPreferences paymentPreferences) {
        this.paymentPreferences = paymentPreferences;
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

        final UpdatePlayerRequest rhs = (UpdatePlayerRequest) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(pictureLocation, rhs.pictureLocation)
                .append(displayName, rhs.displayName)
                .append(paymentPreferences, rhs.paymentPreferences)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceId)
                .append(BigDecimals.strip(playerId))
                .append(pictureLocation)
                .append(displayName)
                .append(paymentPreferences)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
