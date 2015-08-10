package com.yazino.web.payment.facebook;


public final class TransactionResponse {
    private final String internalTransactionId;
    private final FunctionResultCode functionResultCode;
    private final String errorCode;
    private final String message;
    private final String externalTransactionId;
    private final String responseXMLBody;

    public TransactionResponse(final String internalTransactionId,
                               final FunctionResultCode functionResultCode) {
        this(internalTransactionId, functionResultCode, null, "", "", "");
    }

    public TransactionResponse(final String internalTransactionId,
                               final FunctionResultCode functionResultCode,
                               final String message) {
        this(internalTransactionId, functionResultCode, null, message, "", "");
    }

    public TransactionResponse(final String internalTransactionId,
                               final FunctionResultCode functionResultCode,
                               final String errorCode,
                               final String message,
                               final String externalTransactionId,
                               final String responseXMLBody) {
        this.internalTransactionId = internalTransactionId;
        this.functionResultCode = functionResultCode;
        this.errorCode = errorCode;
        this.message = message;
        this.externalTransactionId = externalTransactionId;
        this.responseXMLBody = responseXMLBody;
    }

    public String getInternalTransactionId() {
        return internalTransactionId;
    }

    public FunctionResultCode getPurchaseResponseCode() {
        return functionResultCode;
    }

    public String getMessage() {
        return message;
    }

    public boolean wasSuccessful() {
        return !functionResultCode.equals(FunctionResultCode.NOK);
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public String getResponseXMLBody() {
        return responseXMLBody;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
