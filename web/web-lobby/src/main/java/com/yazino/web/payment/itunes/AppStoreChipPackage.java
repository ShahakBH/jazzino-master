package com.yazino.web.payment.itunes;

import org.apache.commons.lang3.Validate;

import java.math.BigDecimal;

/**
 * I know this looks very similar to AppStoreChipPromotion at the moment;
 * I'm expected more things to be added to that class.
 */
public class AppStoreChipPackage {

    private final String mIdentifier;
    private final BigDecimal mChips;
    private AppStoreChipPromotion mPromotion = null;
    private String mHeader;
    private String mSubHeader;

    public AppStoreChipPackage(final String identifier, final BigDecimal chips) {
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

    public AppStoreChipPromotion getPromotion() {
        return mPromotion;
    }

    public void setPromotion(final AppStoreChipPromotion promotion) {
        mPromotion = promotion;
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
