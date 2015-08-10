package com.yazino.web.payment.googlecheckout;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Products available to player.
 *
 * @deprecated use AndroidStoreProducts instead, when all game clients use /api/1.0/payments/android/google/products or higher
 */
public class AndroidBuyChipStoreConfig {

    public enum ChipBundleKeys {
        DEFAULT_VALUE, GOOGLE_PRODUCT_ID, PROMOTION_CHIPS
    }

    private Long promoId;

    private List<Map<ChipBundleKeys, Object>> chipBundleList;

    public AndroidBuyChipStoreConfig() {
        this.chipBundleList = new ArrayList<Map<ChipBundleKeys, Object>>();
    }

    public List<Map<ChipBundleKeys, Object>> getChipBundleList() {
        return chipBundleList;
    }

    public void setChipBundleList(List<Map<ChipBundleKeys, Object>> chipBundleList) {
        this.chipBundleList = chipBundleList;
    }

    public void addToChipBundleList(final Map<ChipBundleKeys, Object> chipBundle) {
        chipBundleList.add(chipBundle);
    }

    public Long getPromoId() {
        return promoId;
    }

    public void setPromoId(final Long promoId) {
        this.promoId = promoId;
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
        final AndroidBuyChipStoreConfig rhs = (AndroidBuyChipStoreConfig) obj;
        return new EqualsBuilder()
                .append(promoId, rhs.promoId)
                .append(chipBundleList, rhs.chipBundleList)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(promoId)
                .append(chipBundleList)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(promoId)
                .append(chipBundleList)
                .toString();
    }

}
