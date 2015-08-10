package com.yazino.web.payment.googlecheckout;

import java.math.BigDecimal;

public class VerifiedOrderBuilder {
    private String orderId;
    private OrderStatus status;
    private String productId;
    private String currencyCode;
    private BigDecimal price;
    private BigDecimal chips;
    private BigDecimal defaultChips;

    public VerifiedOrderBuilder withOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public VerifiedOrderBuilder withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public VerifiedOrderBuilder withProductId(String productId) {
        this.productId = productId;
        return this;
    }

    public VerifiedOrderBuilder withCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
        return this;
    }

    public VerifiedOrderBuilder withPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public VerifiedOrderBuilder withChips(BigDecimal chips) {
        this.chips = chips;
        return this;
    }

    public VerifiedOrderBuilder withDefaultChips(BigDecimal defaultChips) {
        this.defaultChips = defaultChips;
        return this;
    }

    public VerifiedOrder buildVerifiedOrder() {
        VerifiedOrder verifiedOrder = new VerifiedOrder();
        verifiedOrder.setChips(chips);
        verifiedOrder.setCurrencyCode(currencyCode);
        verifiedOrder.setOrderId(orderId);
        verifiedOrder.setPrice(price);
        verifiedOrder.setProductId(productId);
        verifiedOrder.setStatus(status);
        verifiedOrder.setDefaultChips(defaultChips);
        return verifiedOrder;
    }
}