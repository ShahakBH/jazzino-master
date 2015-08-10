package com.yazino.platform.gifting;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.math.BigDecimal;

@JsonSerialize
public class GiftableStatus implements Serializable {

    private BigDecimal playerId;
    private Giftable giftable;
    private String urlForImage;
    private String displayName;

    public GiftableStatus(final BigDecimal playerId, final Giftable giftable, final String urlForImage, final String displayName) {
        this.playerId = playerId;
        this.giftable = giftable;
        this.urlForImage = urlForImage;
        this.displayName = displayName;
    }

    public GiftableStatus() {
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public Giftable getGiftable() {
        return giftable;
    }

    /**
     * @deprecated no longer provided; this method is retained only for serialisation compatibility and will be removed.
     */
    public String getUrlForImage() {
        return urlForImage;
    }

    /**
     * @deprecated no longer provided; this method is retained only for serialisation compatibility and will be removed.
     */
    public String getDisplayName() {
        return displayName;
    }

    public Giftable isGiftable() {
        return giftable;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public void setGiftable(final Giftable giftable) {
        this.giftable = giftable;
    }

    public void setUrlForImage(final String urlForImage) {
        this.urlForImage = urlForImage;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        GiftableStatus rhs = (GiftableStatus) obj;
        return new EqualsBuilder()
                .append(this.giftable, rhs.giftable)
                .append(this.urlForImage, rhs.urlForImage)
                .append(this.displayName, rhs.displayName)
                .isEquals() && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(giftable)
                .append(urlForImage)
                .append(displayName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("playerId", playerId)
                .append("giftable", giftable)
                .append("urlForImage", urlForImage)
                .append("displayName", displayName)
                .toString();
    }
}
