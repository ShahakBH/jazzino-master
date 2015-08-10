package com.yazino.payment.worldpay.nvp;

public class PaymentTrustDepositMessage extends PaymentTrustMessage {
    private static final long serialVersionUID = -2630841527003039315L;

    {
        defineField("OrderNumber", NVPType.ALPHANUMERIC, null, 35, false);
        defineField("CurrencyId", NVPType.NUMERIC, true);
        defineField("Amount", NVPType.NUMERIC, true);
        defineField("PTTID", NVPType.NUMERIC, null, null, false);
        defineField("ICCCryptogram", NVPType.ALPHANUMERIC, null, 16, false);
        defineField("ICCTerminalResult", NVPType.ALPHANUMERIC, null, 10, false);
        defineField("ICCTrxStatus", NVPType.ALPHANUMERIC, null, 4, false);
        defineField("NarrativeStatement1", NVPType.ALPHANUMERIC, null, 50, false);
        defineField("NarrativeStatement2", NVPType.ALPHANUMERIC, null, 50, false);
        defineField("BLID", NVPType.NUMERIC, null, null, false);
        defineField("Comments", NVPType.ALPHANUMERIC, null, 200, false);
        defineField("MerchantReference", NVPType.ALPHANUMERIC, null, 30, false);
        defineField("MerchantReference2", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("EmployeeId", NVPType.ALPHANUMERIC, null, 16, false);
        defineField("TRXShiftNumber", NVPType.NUMERIC, null, 11, false);

        withValue("RequestType", "D");
    }
}
