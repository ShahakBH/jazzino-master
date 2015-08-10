package com.yazino.payment.worldpay.nvp;

public class PaymentTrustCancellationMessage extends PaymentTrustMessage {
    private static final long serialVersionUID = 1861425655536210121L;

    {
        defineField("OrderNumber", NVPType.ALPHANUMERIC, null, 35, false);
        defineField("PTTID", NVPType.NUMERIC, false);
        defineField("Comments", NVPType.ALPHANUMERIC, null, 200, false);
        defineField("BLID", NVPType.NUMERIC, false);
        defineField("MerchantReference", NVPType.ALPHANUMERIC, null, 30, false);
        defineField("MerchantReference2", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("EmployeeId", NVPType.ALPHANUMERIC, null, 2, false);
        defineField("TRXShiftNumber", NVPType.NUMERIC, null, 11, false);

        withValue("RequestType", "C");
    }
}
