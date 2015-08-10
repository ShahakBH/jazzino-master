package com.yazino.web.payment.itunes;

import com.google.common.base.Function;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.platform.community.PaymentPreferences;
import org.apache.commons.lang3.Validate;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.Validate.notNull;

/**
 * Transforms a {@link com.yazino.bi.payment.PaymentOption} into a {@link AppStoreChipPackage}.
 */
public class AppStorePaymentOptionTransformer implements Function<PaymentOption, AppStoreChipPackage> {

    private static final DecimalFormat EXTENSION_FORMATTER = extensionFormatter();
    private static final DecimalFormat EXTRA_FORMATTER = extraFormatter();
    private static final DecimalFormat CHIP_FORMATTER = chipFormatter();

    private Map<String, String> mHeaders = new HashMap<>();
    private Map<String, String> mSubHeaders = new HashMap<>();
    private final Map<String, String> mIdentifierMappings;

    public AppStorePaymentOptionTransformer(final Map<String, String> mappings) {
        notNull(mappings);
        mIdentifierMappings = mappings;
    }

    @Override
    public AppStoreChipPackage apply(final PaymentOption option) {
        if (option == null) {
            return null;
        }
        final String itunesMethod = PaymentPreferences.PaymentMethod.ITUNES.name();
        final String identifier = option.getId();
        final String mappedIdentifier = lookupMapping(identifier);
        final BigDecimal normalChips = option.getNumChipsPerPurchase();
        final AppStoreChipPackage chipPackage = new AppStoreChipPackage(mappedIdentifier, normalChips);
        if (mHeaders.containsKey(identifier)) {
            chipPackage.setHeader(mHeaders.get(identifier));
        }
        if (mSubHeaders.containsKey(identifier)) {
            chipPackage.setSubHeader(mSubHeaders.get(identifier));
        }

        if (option.hasPromotion(itunesMethod)) {
            final BigDecimal promoChips = option.getNumChipsPerPurchase(itunesMethod);
            final BigDecimal percent = calculatePromotionPercent(normalChips, promoChips);
            if (percent.compareTo(BigDecimal.ZERO) != 0) {
                final String extension = EXTENSION_FORMATTER.format(percent);
                final String promotionalIdentifier = identifier + extension;
                final String promotionalMappedIdentifier = lookupMapping(promotionalIdentifier);
                final AppStoreChipPromotion promotion = new AppStoreChipPromotion(
                        promotionalMappedIdentifier, promoChips);
                promotion.setHeader(EXTRA_FORMATTER.format(percent));
                promotion.setSubHeader(CHIP_FORMATTER.format(promoChips));
                chipPackage.setPromotion(promotion);
            }
        }
        return chipPackage;
    }

    public void setStandardProductHeaders(final Map<String, String> headers) {
        Validate.notNull(headers);
        mHeaders = headers;
    }

    public void setStandardProductSubHeaders(final Map<String, String> subHeaders) {
        Validate.notNull(subHeaders);
        mSubHeaders = subHeaders;
    }

    private String lookupMapping(final String identifier) {
        if (mIdentifierMappings.containsKey(identifier)) {
            return mIdentifierMappings.get(identifier);
        } else {
            return identifier;
        }
    }

    private static BigDecimal calculatePromotionPercent(final BigDecimal normalChips,
                                                        final BigDecimal promoChips) {
        final boolean differ = normalChips.compareTo(promoChips) != 0;
        if (differ) {
            // assuming promochips is always bigger than normal chips
            final BigDecimal extra = promoChips.subtract(normalChips);
            final BigDecimal fraction = extra.divide(normalChips);
            return fraction.multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    private static DecimalFormat extensionFormatter() {
        final DecimalFormat format = new DecimalFormat("_X#");
        format.setMaximumFractionDigits(0);
        return format;
    }

    private static DecimalFormat extraFormatter() {
        final DecimalFormat format = new DecimalFormat("#'%' EXTRA");
        format.setMaximumFractionDigits(0);
        return format;
    }

    private static DecimalFormat chipFormatter() {
        final DecimalFormat format = new DecimalFormat("# chips");
        format.setMaximumFractionDigits(0);
        format.setGroupingSize(3);
        format.setGroupingUsed(true);
        return format;
    }

}
