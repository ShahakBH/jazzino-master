package com.yazino.payment.worldpay.nvp;

public class RedirectAddMessage extends NVPMessage {
    private static final long serialVersionUID = -1236029522984963210L;

    {
        defineField("RequestType", NVPType.ALPHANUMERIC, 1, 1, true);
        defineField("OrderNumber", NVPType.ALPHANUMERIC, null, 35, false);
        defineField("CustomerId", NVPType.ALPHANUMERIC, null, 35, true);
        defineField("IsDefault", NVPType.NUMERIC, 1, 1, false);
        defineField("AcctName", NVPType.ALPHANUMERIC, null, 60, true);
        defineField("AcctNumber", NVPType.NUMERIC, true);
        defineField("ExpDate", NVPType.NUMERIC, false);

        withValue("VersionUsed", 1);
        withValue("TransactionType", "RD");
        withValue("RequestType", "A");
        withValue("TimeOut", 60000);
    }
}
