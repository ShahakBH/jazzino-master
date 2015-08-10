package com.yazino.payment.worldpay.nvp;

public class RedirectUpdateMessage extends NVPMessage {
    private static final long serialVersionUID = -4434025513987234261L;

    {
        defineField("RequestType", NVPType.ALPHANUMERIC, 1, 1, true);
        defineField("OrderNumber", NVPType.ALPHANUMERIC, null, 35, false);
        defineField("CustomerId", NVPType.ALPHANUMERIC, null, 35, true);
        defineField("CardId", NVPType.NUMERIC, null, null, true);
        defineField("IsDefault", NVPType.NUMERIC, 1, 1, false);
        defineField("AcctName", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("ExpDate", NVPType.NUMERIC, false);

        withValue("VersionUsed", 1);
        withValue("TransactionType", "RD");
        withValue("RequestType", "U");
        withValue("TimeOut", 60000);
    }
}
