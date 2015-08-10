package com.yazino.promotions;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import strata.server.lobby.api.promotion.PromotionType;

import java.util.Map;

public class DailyAwardForm extends PromotionForm {
    public static final String PROMO_KEY = "DailyAward.";

    public DailyAwardForm() {
        setPromoType(PromotionType.DAILY_AWARD);
    }

    private Integer topUpAmount;

    public Integer getTopUpAmount() {
        return topUpAmount;
    }

    public void setTopUpAmount(final Integer topUpAmount) {
        this.topUpAmount = topUpAmount;
    }

    @Override
    public String getPromoKey() {
        return PROMO_KEY;
    }

    public Map<String, String> toStringMap() {
        Map<String, String> dailyAwardFormMap = super.toStringMap();
        dailyAwardFormMap.put(getPromoKey() + "topUpAmount", getValueAsStringWithNullCheck(getTopUpAmount()));
        return dailyAwardFormMap;
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
        DailyAwardForm rhs = (DailyAwardForm) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(this.topUpAmount, rhs.topUpAmount)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(topUpAmount)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("topUpAmount", topUpAmount)
                .toString();
    }
}
