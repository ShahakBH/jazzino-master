package com.yazino.web.domain.payment;

import org.apache.commons.lang3.StringUtils;

public class CardRegistrationResult {
    public static final int EXPECTED_DATE_LENGTH = 6;
    private final String cardId;
    private final String customerId;
    private final String obscuredCardNumber;
    private final String customerName;
    private final String expiryMonth;
    private final String expiryYear;
    private final String messageCode;
    private final String message;

    public String getCardId() {
        return cardId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getObscuredCardNumber() {
        return obscuredCardNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public String getMessage() {
        return message;
    }

    public String getExpiryYear() {
        return expiryYear;
    }

    public String getExpiryMonth() {
        return expiryMonth;
    }

    private static String extractExpiryYear(String expiryDate) {
        if (StringUtils.isNotBlank(expiryDate) && expiryDate.length() == EXPECTED_DATE_LENGTH) {
            return expiryDate.substring(2);
        }
        return null;
    }

    private static String extractExpiryMonth(String expiryDate) {
        if (StringUtils.isNotBlank(expiryDate) && expiryDate.length() == EXPECTED_DATE_LENGTH) {
            return expiryDate.substring(0, 2);
        }
        return null;
    }

    CardRegistrationResult(final String cardId,
                           final String customerId,
                           final String obscuredCardNumber,
                           final String customerName,
                           final String expiryDate,
                           final String messageCode,
                           final String message) {
        this.cardId = cardId;
        this.customerId = customerId;
        this.obscuredCardNumber = obscuredCardNumber;
        this.customerName = customerName;
        this.expiryMonth = extractExpiryMonth(expiryDate);
        this.expiryYear = extractExpiryYear(expiryDate);
        this.messageCode = messageCode;
        this.message = message;
    }
}
