package com.yazino.web.controller;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yazino.platform.gifting.AppToUserGift;
import com.yazino.platform.gifting.Gift;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;
import java.util.Set;

@JsonSerialize
public class AllGifts {

    private final List<AppToUserGift> appToUserGifts;
    private final Set<Gift> userToUserGifts;

    public AllGifts(final List<AppToUserGift> appToUserGifts, final Set<Gift> userToUserGifts) {

        this.appToUserGifts = appToUserGifts;
        this.userToUserGifts = userToUserGifts;
    }

    public List<AppToUserGift> getAppToUserGifts() {
        return appToUserGifts;
    }

    public Set<Gift> getUserToUserGifts() {
        return userToUserGifts;
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
        AllGifts rhs = (AllGifts) obj;
        return new EqualsBuilder()
                .append(this.appToUserGifts, rhs.appToUserGifts)
                .append(this.userToUserGifts, rhs.userToUserGifts)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(appToUserGifts)
                .append(userToUserGifts)
                .toHashCode();
    }
}
