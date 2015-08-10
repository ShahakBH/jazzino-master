package com.yazino.payment.worldpay.nvp;

public class RedirectGenerateMessage extends NVPMessage {
    private static final long serialVersionUID = -3336025522984934260L;

    {
        defineField("RequestType", NVPType.ALPHANUMERIC, true);
        defineField("Action", NVPType.ALPHANUMERIC, true);
        defineField("OrderNumber", NVPType.ALPHANUMERIC, null, 35, false);
        defineField("CustomerId", NVPType.ALPHANUMERIC, null, 35, false);
        defineField("CardId", NVPType.NUMERIC, null, null, false);
        defineField("IsVerify", NVPType.NUMERIC, null, null, false);
        defineField("OTTRegion", NVPType.NUMERIC, null, null, false);
        defineField("OTTResultURL", NVPType.ALPHANUMERIC, null, 255, true);

        withValue("VersionUsed", 1);
        withValue("TransactionType", "RD");
        withValue("RequestType", "G");
        withValue("TimeOut", 60000);
    }
}
