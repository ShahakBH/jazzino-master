package com.yazino.web.payment.facebook;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.bi.payment.PaymentOption;

import java.math.BigDecimal;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class FacebookProductUrl {
    final static String key = "/fbog/product/";
    private final String path;
    private final String productPackage;
    private final String webPackage;
    private final BigDecimal cashValue;
    private final Long promoId;
    private final Integer chips;
    private final String packageNumber;

    public FacebookProductUrl(final String productUrl) throws WalletServiceException {
        if (isBlank(productUrl)) {
            throw new WalletServiceException("Product Cannot Be Blank");
        }
        final String[] parts = productUrl.split(key);
        if (parts.length != 2 || isBlank(parts[0]) || isBlank(parts[1])) {
            throw new WalletServiceException("url doesn't contain key parts " + productUrl);

        }
        final String[] product = parts[1].split("_");
        if (product.length < 4 || product.length > 5 || !product[2].equals("buys") || !product[3].contains("k")) {
            throw new WalletServiceException("url doesn't contain key parts " + productUrl);
        }

        if (product[0].length() != 4 || !product[0].matches("[a-zA-Z]{3}[0-9]{1}")) {
            throw new WalletServiceException("invalid currency package: " + product[0]);
        }

        try {
            path = parts[0] + key;
            productPackage = product[0];
            webPackage = "option" + productPackage.toUpperCase();
            packageNumber = productPackage.substring(3);
            cashValue = new BigDecimal(product[1]);
            chips = Integer.parseInt(product[3].replace("k", "000"));
            promoId = (product.length > 4) ? Long.parseLong(product[4]) : null;


        } catch (Exception e) {
            throw new WalletServiceException("Problem creating FacebookProductUrl" + e);
        }
    }

    public FacebookProductUrl(final PaymentOption paymentOption, final FacebookProductUrl product) {
        path = product.getPath();
        productPackage = paymentOption.getRealMoneyCurrency().toLowerCase() + product.getPackageNumber();
        webPackage = "option" + productPackage.toUpperCase();
        packageNumber = productPackage.substring(3);
        cashValue = new BigDecimal(paymentOption.getAmountRealMoneyPerPurchase().toString());
        chips = paymentOption.getNumChipsPerPurchase("FACEBOOK").intValue();
        if (paymentOption.getPromotion(PaymentPreferences.PaymentMethod.FACEBOOK) != null) {
            promoId = paymentOption.getPromotion(PaymentPreferences.PaymentMethod.FACEBOOK).getPromoId();
        } else {
            promoId = null;
        }
    }

    public String getPackageNumber() {
        return packageNumber;
    }

    public String getPath() {
        return path;
    }

    public String getPackage() {
        return productPackage;
    }

    public String getWebPackage() {
        return webPackage;
    }

    public BigDecimal getCashValue() {
        return cashValue;
    }

    public Long getPromoId() {
        return promoId;
    }

    public static FacebookProductUrl fromProductIdWithoutUrl(final String productId) throws WalletServiceException {
        return new FacebookProductUrl("padding" + FacebookProductUrl.key + productId);
    }

    public Integer getChips() {
        return chips;
    }

    public String toUrl() {
        StringBuilder rebuiltProduct = new StringBuilder(getPath());
        rebuiltProduct
                .append(getPackage())
                .append("_")
                .append(getCashValue())
                .append("_buys_")
                .append(getChips() / 1000)
                .append("k");
        if (getPromoId() != null) {
            rebuiltProduct.append("_").append(getPromoId());
        }
        return rebuiltProduct.toString();

    }
}
