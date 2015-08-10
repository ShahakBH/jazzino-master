package com.yazino.payment.worldpay.nvp;

public abstract class PaymentTrustMessage extends NVPMessage {
    private static final long serialVersionUID = 790495658766645525L;

    static final int TRX_SOURCE_WEB = 4;

    {
        defineField("CurrencyId", NVPType.NUMERIC, false);
        defineField("RequestType", NVPType.ALPHANUMERIC, null, 1, true);

        withValue("VersionUsed", 3);
        withValue("TransactionType", "PT");
        withValue("TimeOut", 60000);
    }
}
