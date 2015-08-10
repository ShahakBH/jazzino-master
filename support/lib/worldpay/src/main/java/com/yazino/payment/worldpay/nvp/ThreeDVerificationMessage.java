package com.yazino.payment.worldpay.nvp;

public class ThreeDVerificationMessage extends NVPMessage {
    private static final long serialVersionUID = 3148204328507106842L;

    {
        defineField("RequestType", NVPType.ALPHANUMERIC, true);
        defineField("OrderNumber", NVPType.ALPHANUMERIC, null, 35, false);
        defineField("AcctNumber", NVPType.NUMERIC, true);
        defineField("ExpDate", NVPType.NUMERIC, 6, 6, true);
        defineField("Amount", NVPType.NUMERIC, true);
        defineField("CurrencyId", NVPType.NUMERIC, false);
        defineField("SecureId", NVPType.ALPHANUMERIC, 20, 20, false);
        defineField("PurchaseDesc", NVPType.ALPHANUMERIC, null, 125, false);

        withValue("VersionUsed", 3);
        withValue("TransactionType", "3D");
        withValue("RequestType", "V");
        withValue("TimeOut", 60000);
    }
}
