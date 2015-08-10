package com.yazino.web.payment.itunes;

import org.apache.commons.lang3.Validate;

import java.math.BigDecimal;

public class AppStoreChipPromotion {

    private final String mIdentifier;
    private final BigDecimal mChips;
    private String mHeader;
    private String mSubHeader;

    public AppStoreChipPromotion(final String identifier, final BigDecimal chips) {
        Validate.notNull(identifier);
        Validate.notNull(chips);
        mIdentifier = identifier;
        mChips = chips;
    }

    public String getIdentifier() {
        return mIdentifier;
    }

    public BigDecimal getChips() {
        return mChips;
    }

    public String getHeader() {
        return mHeader;
    }

    public void setHeader(final String header) {
        mHeader = header;
    }

    public String getSubHeader() {
        return mSubHeader;
    }

    public void setSubHeader(final String subHeader) {
        mSubHeader = subHeader;
    }
}
