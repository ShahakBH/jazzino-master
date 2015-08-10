package com.yazino.platform.gifting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;

@JsonSerialize
public class PlayerCollectionStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty
    private int collectionsRemainingForCurrentPeriod;
    @JsonProperty
    private int giftsWaitingToBeCollected;

    @JsonCreator
    public PlayerCollectionStatus(@JsonProperty Integer collectionsRemainingForCurrentPeriod,
                                  @JsonProperty Integer giftsWaitingToBeCollected) {
        this.collectionsRemainingForCurrentPeriod = collectionsRemainingForCurrentPeriod;
        this.giftsWaitingToBeCollected = giftsWaitingToBeCollected;
    }

    public int getCollectionsRemainingForCurrentPeriod() {
        return collectionsRemainingForCurrentPeriod;
    }

    public int getGiftsWaitingToBeCollected() {
        return giftsWaitingToBeCollected;
    }

    public int collectionsThatCanBeMadeImmediately() {
        return Math.min(collectionsRemainingForCurrentPeriod, giftsWaitingToBeCollected);
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
        PlayerCollectionStatus rhs = (PlayerCollectionStatus) obj;
        return new EqualsBuilder()
                .append(this.collectionsRemainingForCurrentPeriod, rhs.collectionsRemainingForCurrentPeriod)
                .append(this.giftsWaitingToBeCollected, rhs.giftsWaitingToBeCollected)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(collectionsRemainingForCurrentPeriod)
                .append(giftsWaitingToBeCollected)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("collectionsRemainingForCurrentPeriod", collectionsRemainingForCurrentPeriod)
                .append("giftsWaitingToBeCollected", giftsWaitingToBeCollected)
                .toString();
    }
}
