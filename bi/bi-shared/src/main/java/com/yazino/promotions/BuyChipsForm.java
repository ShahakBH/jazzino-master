package com.yazino.promotions;

import com.yazino.platform.community.PaymentPreferences;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import strata.server.lobby.api.promotion.PromotionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuyChipsForm extends PromotionForm {

    public static final String PROMO_KEY = "BuyChips.";
    private String inGameNotificationMsg;
    private String inGameNotificationHeader;
    private String rolloverHeader;
    private String rolloverText;

    private List<PaymentPreferences.PaymentMethod> paymentMethods = new ArrayList<PaymentPreferences.PaymentMethod>();

    private Map<Integer, BigDecimal> chipsPackagePercentages = new LinkedHashMap<Integer, BigDecimal>();

    // Deafault constructor needs to be here for spring binding if there is another constructor present
    public BuyChipsForm() {
        setPromoType(PromotionType.BUY_CHIPS);
    }

    public String getInGameNotificationMsg() {
        return inGameNotificationMsg;
    }

    public void setInGameNotificationMsg(final String inGameNotificationMsg) {
        this.inGameNotificationMsg = inGameNotificationMsg;
    }

    public String getInGameNotificationHeader() {
        return inGameNotificationHeader;
    }

    public void setInGameNotificationHeader(final String inGameNotificationHeader) {
        this.inGameNotificationHeader = inGameNotificationHeader;
    }

    public String getRolloverHeader() {
        return rolloverHeader;
    }

    public void setRolloverHeader(final String rolloverHeader) {
        this.rolloverHeader = rolloverHeader;
    }

    public String getRolloverText() {
        return rolloverText;
    }

    public void setRolloverText(final String rolloverText) {
        this.rolloverText = rolloverText;
    }

    public List<PaymentPreferences.PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentPreferences.PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public Map<Integer, BigDecimal> getChipsPackagePercentages() {
        return chipsPackagePercentages;
    }

    public void setChipsPackagePercentages(Map<Integer, BigDecimal> chipsPackagePercentages) {
        this.chipsPackagePercentages = chipsPackagePercentages;
    }

    @Override
    public String getPromoKey() {
        return PROMO_KEY;
    }

    public Map<String, String> toStringMap() {
        Map<String, String> buyChipsFormMap = super.toStringMap();
        buyChipsFormMap.put(PROMO_KEY, "");
        buyChipsFormMap.put(PROMO_KEY + "promotionDefinitionId", getValueAsStringWithNullCheck(getPromotionDefinitionId()));
        buyChipsFormMap.put(PROMO_KEY + "inGameNotificationMsg", getValueAsStringWithNullCheck(inGameNotificationMsg));
        buyChipsFormMap.put(PROMO_KEY + "inGameNotificationHeader", getValueAsStringWithNullCheck(inGameNotificationHeader));
        buyChipsFormMap.put(PROMO_KEY + "rolloverHeader", getValueAsStringWithNullCheck(rolloverHeader));
        buyChipsFormMap.put(PROMO_KEY + "rolloverText", getValueAsStringWithNullCheck(rolloverText));
        buyChipsFormMap.put(PROMO_KEY + "chipsPackagePercentages", getValueAsStringWithNullCheck(chipsPackagePercentages));
        return buyChipsFormMap;
    }



    public boolean isPaymentMethodEnabled(String name) {
        return paymentMethods.contains(PaymentPreferences.PaymentMethod.valueOf(name));
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
        BuyChipsForm rhs = (BuyChipsForm) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(this.PROMO_KEY, rhs.PROMO_KEY)
                .append(this.inGameNotificationMsg, rhs.inGameNotificationMsg)
                .append(this.inGameNotificationHeader, rhs.inGameNotificationHeader)
                .append(this.rolloverHeader, rhs.rolloverHeader)
                .append(this.rolloverText, rhs.rolloverText)
                .append(this.paymentMethods, rhs.paymentMethods)
                .append(this.chipsPackagePercentages, rhs.chipsPackagePercentages)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(PROMO_KEY)
                .append(inGameNotificationMsg)
                .append(inGameNotificationHeader)
                .append(rolloverHeader)
                .append(rolloverText)
                .append(paymentMethods)
                .append(chipsPackagePercentages)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("PROMO_KEY", PROMO_KEY)
                .append("inGameNotificationMsg", inGameNotificationMsg)
                .append("inGameNotificationHeader", inGameNotificationHeader)
                .append("rolloverHeader", rolloverHeader)
                .append("rolloverText", rolloverText)
                .append("paymentMethods", paymentMethods)
                .append("chipsPackagePercentages", chipsPackagePercentages)
                .toString();
    }
}
