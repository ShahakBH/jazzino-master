package com.yazino.payment.worldpay.nvp;

public class PaymentTrustSaleMessage extends PaymentTrustMessage {
    private static final long serialVersionUID = -2630841527003039315L;

    private static final int TRX_SOURCE_WEB = 4;

    {
        defineField("AcctNumber2", NVPType.NUMERIC, false);
        defineField("DirectAcceptNumber", NVPType.NUMERIC, false);
        defineField("TXNarrativeStatement1", NVPType.ALPHANUMERIC, null, 50, false);
        defineField("TXNarrativeStatement2", NVPType.ALPHANUMERIC, null, 50, false);
        defineField("RTStartDate", NVPType.NUMERIC, 8, 8, false);
        defineField("RTEndDate", NVPType.NUMERIC, 8, 8, false);
        defineField("RTFrequencyValue", NVPType.NUMERIC, null, null, false);
        defineField("RTFrequencyType", NVPType.ALPHA, null, 10, false);
        defineField("IsRT", NVPType.NUMERIC, 1, 1, false);
        defineField("Comments", NVPType.ALPHANUMERIC, null, 200, false);
        defineField("MerchantBillingNumber", NVPType.ALPHANUMERIC, null, 25, false);

        defineField("MOP", NVPType.ALPHA, null, 2, true);
        defineField("TRXSource", NVPType.NUMERIC, false);
        defineField("Track2Data", NVPType.ALPHANUMERIC, false);
        defineField("OrderNumber", NVPType.ALPHANUMERIC, null, 35, false);
        defineField("AcctName", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("AcctNumber", NVPType.NUMERIC, false);
        defineField("AcctPIN", NVPType.NUMERIC, null, 16, false);
        defineField("ExpDate", NVPType.NUMERIC, 6, 6, false);
        defineField("IssueNumber", NVPType.NUMERIC, false);
        defineField("StartDate", NVPType.NUMERIC, 6, 6, false);
        defineField("CurrencyId", NVPType.NUMERIC, true);
        defineField("FXID", NVPType.NUMERIC, false);
        defineField("Amount", NVPType.NUMERIC, true);
        defineField("Title", NVPType.ALPHANUMERIC, null, 20, false);
        defineField("Company", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("FirstName", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("MiddleName", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("LastName", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("Suffix", NVPType.ALPHANUMERIC, null, 20, false);
        defineField("Address1", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("Address2", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("Address3", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("City", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("StateCode", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("ZipCode", NVPType.ALPHANUMERIC, null, 30, false);
        defineField("CountryCode", NVPType.ALPHA, null, 2, false);
        defineField("PhoneNumber", NVPType.ALPHANUMERIC, null, 30, false);
        defineField("PhoneExtension", NVPType.ALPHANUMERIC, null, 10, false);
        defineField("Email", NVPType.ALPHANUMERIC, null, 50, false);
        defineField("ShipToTitle", NVPType.ALPHANUMERIC, null, 20, false);
        defineField("ShipToFirstName", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("ShipToMiddleName", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("ShipToLastName", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("ShipToSuffix", NVPType.ALPHANUMERIC, null, 20, false);
        defineField("ShipToAddress1", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("ShipToAddress2", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("ShipToAddress3", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("ShipToCity", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("ShipToStateCode", NVPType.ALPHANUMERIC, null, 30, false);
        defineField("ShipToZipCode", NVPType.ALPHANUMERIC, null, 30, false);
        defineField("ShipToCountryCode", NVPType.ALPHA, null, 2, false);
        defineField("ShipToPhoneNumber", NVPType.ALPHANUMERIC, null, 30, false);
        defineField("ShipToPhoneExtension", NVPType.ALPHANUMERIC, null, 10, false);
        defineField("CVN", NVPType.NUMERIC, 3, 4, false);
        defineField("SVID", NVPType.NUMERIC, null, null, false);
        defineField("ECI", NVPType.NUMERIC, null, 2, false);
        defineField("SecureId", NVPType.ALPHANUMERIC, null, 20, false);
        defineField("CAV", NVPType.ALPHANUMERIC, null, 50, false);
        defineField("CHEnrolled", NVPType.ALPHA, null, 1, false);
        defineField("TXStatus", NVPType.ALPHA, null, 1, false);
        defineField("PTTID", NVPType.NUMERIC, null, null, false);
        defineField("BLID", NVPType.NUMERIC, null, null, false);
        defineField("MerchantReference", NVPType.ALPHANUMERIC, null, 30, false);
        defineField("MerchantReference2", NVPType.ALPHANUMERIC, null, 60, false);
        defineField("EmployeeId", NVPType.ALPHANUMERIC, null, 16, false);
        defineField("TRXShiftNumber", NVPType.NUMERIC, null, 11, false);
        defineField("REMOTE_ADDR", NVPType.ALPHANUMERIC, null, 100, false);
        defineField("EncryptedSerialNumber", NVPType.ALPHANUMERIC, null, 20, false);
        defineField("CustomerId", NVPType.ALPHANUMERIC, null, 35, false);
        defineField("CardId", NVPType.NUMERIC, null, null, false);

        withValue("RequestType", "S");
        withValue("MOP", "CC");
        withValue("TRXSource", TRX_SOURCE_WEB);
    }
}
