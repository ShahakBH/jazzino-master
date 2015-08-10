package com.yazino.web.payment.creditcard;

import com.yazino.validation.EmailAddressFormatValidator;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.session.LobbySession;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class CreditCardForm {
    private String paymentOptionId;
    private Long promotionId;
    private String creditCardNumber;
    private String cvc2;
    private String expirationMonth;
    private String expirationYear;
    private String cardHolderName;
    private String cardId;
    private String emailAddress;
    private String termsAndServiceAgreement;
    private String gameType;
    private String obscuredCardNumber;
    private List<String> invalidFields = new ArrayList<>();
    private List<String> missingFields = new ArrayList<>();

    CreditCardForm(final String paymentOptionId,
                   final Long promotionId,
                   final String creditCardNumber,
                   final String cvc2,
                   final String expirationMonth,
                   final String expirationYear,
                   final String cardHolderName,
                   final String cardId,
                   final String emailAddress,
                   final String termsAndServiceAgreement,
                   final String gameType,
                   final String obscuredCardNumber) {
        this.paymentOptionId = paymentOptionId;
        this.promotionId = promotionId;
        this.creditCardNumber = creditCardNumber;
        this.cvc2 = cvc2;
        this.expirationMonth = expirationMonth;
        this.expirationYear = expirationYear;
        this.cardHolderName = cardHolderName;
        this.cardId = cardId;
        this.emailAddress = emailAddress;
        this.termsAndServiceAgreement = termsAndServiceAgreement;
        this.gameType = gameType;
        this.obscuredCardNumber = obscuredCardNumber;
    }

    private CreditCardForm() {
    }

    public String getPaymentOptionId() {
        return paymentOptionId;
    }

    public void setPaymentOptionId(final String paymentOptionId) {
        this.paymentOptionId = paymentOptionId;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(final Long promotionId) {
        this.promotionId = promotionId;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(final String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public String getCvc2() {
        return cvc2;
    }

    public void setCvc2(final String cvc2) {
        this.cvc2 = cvc2;
    }

    public String getExpirationMonth() {
        return expirationMonth;
    }

    public void setExpirationMonth(final String expirationMonth) {
        this.expirationMonth = expirationMonth;
    }

    public String getExpirationYear() {
        return expirationYear;
    }

    public void setExpirationYear(final String expirationYear) {
        this.expirationYear = expirationYear;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(final String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(final String cardId) {
        this.cardId = cardId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getTermsAndServiceAgreement() {
        return termsAndServiceAgreement;
    }

    public void setTermsAndServiceAgreement(final String termsAndServiceAgreement) {
        this.termsAndServiceAgreement = termsAndServiceAgreement;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getObscuredCardNumber() {
        return obscuredCardNumber;
    }

    public void setObscuredCardNumber(final String obscuredCardNumber) {
        this.obscuredCardNumber = obscuredCardNumber;
    }

    public List<String> getInvalidFields() {
        return invalidFields;
    }

    public void setInvalidFields(final List<String> invalidFields) {
        this.invalidFields = invalidFields;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(final List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public PaymentContext toPaymentContext(final LobbySession session,
                                           final String paymentGameType) {
        return new PaymentContext(
                session.getPlayerId(),
                session.getSessionId(), session.getPlayerName(),
                paymentGameType,
                getEmailAddress(),
                getPaymentOptionId(),
                getPromotionId(),
                session.getPartnerId());
    }

    public PaymentContext toPaymentContext(final LobbySession session) {
        return toPaymentContext(session, getGameType());
    }

    public CreditCardDetails toCreditCardDetails() {
        return new CreditCardDetails(
                stripWhiteSpace(getCreditCardNumber()),
                getCvc2(),
                getExpirationMonth(),
                getExpirationYear(),
                getCardHolderName(),
                getCardId(),
                getObscuredCardNumber());
    }

    public boolean isValidForm(final List<String> results) {
        invalidFields.clear();
        missingFields.clear();
        results.clear();

        if (isNotBlank(cardId)) {
            return true;
        }

        final boolean validateString = validateCardHolderName(getCardHolderName());
        final boolean validateCreditCardNumber = validateCreditCardNumber(getCreditCardNumber());
        final boolean validateExpiryMonth = validateExpiryMonth(getExpirationMonth());
        final boolean validateExpiryYear = validateExpiryYear(getExpirationYear());
        final boolean validateExpireDateInFuture = validateExpireDateInFuture(
                getExpirationMonth(), getExpirationYear());
        final boolean validateCVC = validateCVC(getCvc2());
        final boolean validateEmail = validateEmail(getEmailAddress());
        if (invalidFields.size() > 0) {
            final StringBuilder invalidError = new StringBuilder("Invalid ");
            for (int i = 0; invalidFields.size() > i; i++) {
                invalidError.append(invalidFields.get(i));
                if (i != invalidFields.size() - 1) {
                    invalidError.append(", ");
                }
            }
            invalidError.append(" entered.");
            results.add(invalidError.toString());
        }
        if (missingFields.size() > 0) {
            final StringBuilder missingError = new StringBuilder();
            for (int i = 0; missingFields.size() > i; i++) {
                missingError.append(missingFields.get(i));
                if (i != missingFields.size() - 1) {
                    missingError.append(", ");
                }
            }
            missingError.append(" must be entered.");
            results.add(missingError.toString());
        }
        return validateString && validateCreditCardNumber && validateExpiryMonth && validateExpiryYear
                && validateExpireDateInFuture && validateCVC && validateEmail;
    }

    boolean validateExpireDateInFuture(final String testExpirationMonth,
                                       final String testExpirationYear) {
        if (testExpirationMonth == null || testExpirationYear == null) {
            return true;
        }

        final DateTime expireDate = new DateTime(Integer.parseInt(testExpirationYear),
                Integer.parseInt(testExpirationMonth), 1, 23, 59, 59, 0).dayOfMonth().withMaximumValue();
        if (expireDate.isBeforeNow()) {
            invalidFields.add("Expiry Date");
            return false;
        }
        return true;
    }

    boolean validateCreditCardNumber(final String testCreditCardNumber) {
        if (!validateString(testCreditCardNumber)) {
            missingFields.add("Credit Card Number");
            return false;
        }
        final String strippedCC = stripWhiteSpace(testCreditCardNumber);

        if (!strippedCC.matches("\\d{16}") || !isValidCC(strippedCC)) {
            invalidFields.add("Credit Card Number");
            return false;
        }
        return true;
    }

    private boolean isValidCC(final String number) {
        final int[][] sumTable = {{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, {0, 2, 4, 6, 8, 1, 3, 5, 7, 9}};
        int sum = 0, flip = 0;

        for (int i = number.length() - 1; i >= 0; i--) {
            sum += sumTable[flip++ & 0x1][Character.digit(number.charAt(i), 10)];
        }
        return sum % 10 == 0;
    }

    boolean validateExpiryYear(final String expiryYear) {
        return !(expiryYear == null || !expiryYear.matches("\\d{4}"));
    }


    boolean validateExpiryMonth(final String expiryMonth) {
        return !(expiryMonth == null || !expiryMonth.matches("\\d{2}"));
    }

    boolean validateCVC(final String cvc) {
        if (!validateString(cvc)) {
            missingFields.add("Security Code");
            return false;
        }
        if (!(cvc.matches("\\d{3}"))) {
            invalidFields.add("Security Code");
            return false;
        }
        return true;
    }

    boolean validateEmail(final String toTest) {
        if (!validateString(toTest)) {
            missingFields.add("Email Address");
            return false;
        }
        if (!EmailAddressFormatValidator.isValidFormat(toTest)) {
            invalidFields.add("Email Address");
            return false;
        }
        return true;
    }

    boolean validateCardHolderName(final String testCardHolderName) {
        if (!validateString(testCardHolderName)) {
            missingFields.add("Card Holder Name");
            return false;
        }
        return true;
    }

    boolean validateString(final String toTest) {
        return !StringUtils.isBlank(toTest);
    }

    public String stripWhiteSpace(final String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        return input.replaceAll("\\s*", "");
    }

    @Override
    public String toString() {
        return "CreditCardForm{"
                + "paymentOptionId='" + paymentOptionId + '\''
                + ", promotionId=" + promotionId
                + ", creditCardNumber='" + creditCardNumber + '\''
                + ", cvc2='" + cvc2 + '\''
                + ", expirationMonth='" + expirationMonth + '\''
                + ", expirationYear='" + expirationYear + '\''
                + ", cardId='" + cardId + '\''
                + ", cardHolderName='" + cardHolderName + '\''
                + ", emailAddress='" + emailAddress + '\''
                + ", termsAndServiceAgreement='" + termsAndServiceAgreement + '\''
                + ", invalidFields=" + invalidFields
                + ", missingFields=" + missingFields
                + '}';
    }
}
