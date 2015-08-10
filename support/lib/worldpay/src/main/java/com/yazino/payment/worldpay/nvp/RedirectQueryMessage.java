package com.yazino.payment.worldpay.nvp;

public class RedirectQueryMessage extends NVPMessage {
    private static final long serialVersionUID = -1626025528904934273L;

    {
        defineField("RequestType", NVPType.ALPHANUMERIC, null, 1, false);
        defineField("CustomerId", NVPType.ALPHANUMERIC, null, 35, false);

        withValue("RequestType", "Q");
        withValue("VersionUsed", 1);
        withValue("TransactionType", "RD");
        withValue("TimeOut", 60000);
    }
}
