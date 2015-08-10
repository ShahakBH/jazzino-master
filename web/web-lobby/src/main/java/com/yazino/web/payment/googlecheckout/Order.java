package com.yazino.web.payment.googlecheckout;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Summarizes the state of an order. The status is a combination of Google Checkout's financial order state and
 * fulfillment order state.
 * Note that digital goods orders are marked as 'DELIVERED' when the order is charged, see http://support.google.com/checkout/sell/bin/answer.py?hl=en&answer=25397
 *
 * @see Order.Status
 * @deprecated use {@link VerifiedOrder}
 */
public class Order implements Serializable {

    /**
     * Possible order states are:
     * <ul>
     * <li>ERROR - error processing the order</li>
     * <li>UNKNOWN_PRODUCT - the order notification's product id could not be matched with products known to checkout
     * service. Chips have not been credited</li>
     * <li>INVALID_ORDER_NUMBER - the order number is unknown to Google Checkout</li>
     * <li>PAYMENT_NOT_AUTHORIZED - Google Checkout has not authorized the order yet</li>
     * <li>PAYMENT_AUTHORIZED - Google Checkout has authorized or charged the players card, chips can be credited</li>
     * <li>DELIVERED - chips have been credited</l>
     * <li>CANCELLED - Payment declined or order cancelled</li>
     * </ul>
     */
    public enum Status {
        /**
         * error handling the fulfillment - client can try again
         */
        ERROR,

        /**
         * the google checkout product id, could not be matched to currently available products. Player should contact
         * customer services
         */
        UNKNOWN_PRODUCT,

        /**
         * the order number is unknown to Google Checkout.
         */
        INVALID_ORDER_NUMBER,

        /**
         * Google Checkout hasn't authorized the order yet
         */
        PAYMENT_NOT_AUTHORIZED,

        /**
         * Order is authorized and can be fulfilled
         */
        PAYMENT_AUTHORIZED,

        /**
         * Processing of the order has already started
         */
        IN_PROGRESS,

        /**
         * Order has been fulfilled and chips have been credited to the player
         */
        DELIVERED,

        /**
         * Payment declined or order cancelled
         */
        CANCELLED
    }

    private String orderNumber;
    private Status status;

    // currency code, price and product id taken from Google Checkout notifications
    private String productId;
    private String currencyCode;
    private BigDecimal price;
    // chips populated by looking up payment option
    private BigDecimal chips;

    public Order() {
    }

    public Order(final String orderNumber, final Status status) {
        this.orderNumber = orderNumber;
        this.status = status;
    }

    public BigDecimal getChips() {
        return chips;
    }

    public void setChips(final BigDecimal chips) {
        this.chips = chips;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(final String productId) {
        this.productId = productId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(final String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price;
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
        final Order rhs = (Order) obj;
        return new EqualsBuilder()
                .append(orderNumber, rhs.orderNumber)
                .append(status, rhs.status)
                .append(productId, rhs.productId)
                .append(currencyCode, rhs.currencyCode)
                .append(price, rhs.price)
                .append(chips, rhs.chips)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(orderNumber)
                .append(status)
                .append(productId)
                .append(currencyCode)
                .append(price)
                .append(chips)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE)
                .append(orderNumber)
                .append(status)
                .append(productId)
                .append(currencyCode)
                .append(price)
                .append(chips)
                .toString();
    }
}
