package com.yazino.payment.worldpay.nvp;

public class ThreeDAuthenticateMessage extends NVPMessage {
    private static final long serialVersionUID = -1756055527010931460L;

    {
        defineField("RequestType", NVPType.ALPHANUMERIC, true);
        defineField("OrderNumber", NVPType.ALPHANUMERIC, null, 35, false);
        defineField("PaRes", NVPType.ALPHANUMERIC, null, 5000, true);
        defineField("SVID", NVPType.NUMERIC, false);

        withValue("VersionUsed", 3);
        withValue("TransactionType", "3D");
        withValue("RequestType", "A");
        withValue("TimeOut", 60000);
    }
}
