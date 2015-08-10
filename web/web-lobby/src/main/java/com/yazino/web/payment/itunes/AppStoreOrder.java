package com.yazino.web.payment.itunes;

import com.yazino.platform.community.PaymentPreferences;
import org.apache.commons.lang.builder.ToStringBuilder;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

class AppStoreOrder extends Order {

    private static final int SUCCESS_STATUS_CODE = 0;
    private static final String CASHIER = "iTunes";
    private static final String SANDBOX_CASHIER = "iTunesSandbox";

    private final int mStatusCode;
    private boolean mProcessed = false;

    public AppStoreOrder(final int statusCode) {
        this(statusCode,false);
    }
    public AppStoreOrder(final int statusCode,boolean isSandbox) {
        super(isSandbox ? SANDBOX_CASHIER : CASHIER, PaymentPreferences.PaymentMethod.ITUNES);
        mStatusCode = statusCode;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public boolean isProcessed() {
        return mProcessed;
    }

    public void setProcessed(final boolean processed) {
        mProcessed = processed;
    }

    public boolean isValid() {
        return mStatusCode == SUCCESS_STATUS_CODE;
    }

    public boolean matchesContext(final AppStorePaymentContext context) {
        final String contextProductIdentifier = context.getProductIdentifier();
        final String orderProductIdentifier = getProductId();
        final String contextTransactionIdentifier = context.getTransactionIdentifier();
        final String orderContextIdentifier = getOrderId();

        final boolean productIdentifiersMatch = isNotBlank(contextProductIdentifier)
                && isNotBlank(orderProductIdentifier)
                && equalsIgnoreCase(contextProductIdentifier, orderProductIdentifier);
        final boolean transactionIdentifiersMatch = isNotBlank(contextTransactionIdentifier)
                && isNotBlank(orderContextIdentifier)
                && equalsIgnoreCase(contextTransactionIdentifier, orderContextIdentifier);
        return productIdentifiersMatch && transactionIdentifiersMatch;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("statusCode", mStatusCode);
        builder.append("processed", mProcessed);
        builder.appendSuper(super.toString());
        return builder.toString();
    }
}
