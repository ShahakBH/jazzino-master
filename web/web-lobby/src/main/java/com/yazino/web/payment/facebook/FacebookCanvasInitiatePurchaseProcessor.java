package com.yazino.web.payment.facebook;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Platform;
import com.yazino.web.payment.TransactionIdGenerator;
import com.yazino.web.payment.amazon.InitiatePurchaseProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static java.lang.String.format;

@Service
public class FacebookCanvasInitiatePurchaseProcessor implements InitiatePurchaseProcessor {

    private final static String PRODUCT_URL_TEMPLATE = "%s/fbog/product/%s"; // opengraphhost/fbog/product/productId
    private TransactionIdGenerator transactionIdGenerator;
    private YazinoConfiguration configuration;

    @Autowired
    public FacebookCanvasInitiatePurchaseProcessor(final TransactionIdGenerator transactionIdGenerator, final YazinoConfiguration configuration) {
        this.transactionIdGenerator = transactionIdGenerator;
        this.configuration = configuration;
    }

    @Override
    public Object initiatePurchase(final BigDecimal playerId,
                                   final String productId,
                                   final Long promotionId,
                                   final String gameType,
                                   final Platform platform) {

        return buildFacebookInitiatePaymentResponse(productId);
    }

    private FacebookInitiatePaymentResponse buildFacebookInitiatePaymentResponse(final String productId) {
        return new FacebookInitiatePaymentResponse(String.valueOf(transactionIdGenerator.generateNumericTransactionId()),
                                                   buildProductUrl(productId));
    }

    @Override
    public Platform getPlatform() {
        return Platform.FACEBOOK_CANVAS;
    }

    private String buildProductUrl(String productId) {
        return format(PRODUCT_URL_TEMPLATE, configuration.getString("facebook.openGraphObjectsHost"), productId);
    }


}
