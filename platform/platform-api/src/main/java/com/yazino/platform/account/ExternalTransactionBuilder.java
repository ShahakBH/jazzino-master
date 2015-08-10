package com.yazino.platform.account;

import com.yazino.platform.Platform;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.Validate.notNull;

public class ExternalTransactionBuilder {
    private static final int CARD_DIGITS_TO_RETAIN = 4;
    private static final char NUMBER_OBSCURED_CHARACTER = 'X';

    private final Pattern creditCardPattern = Pattern.compile("([^\\d]\\d{4})\\d{8}(\\d{4}[^\\d])");

    private ExternalTransactionStatus status = ExternalTransactionStatus.REQUEST;
    private ExternalTransactionType type = ExternalTransactionType.DEPOSIT;
    private BigDecimal accountId;
    private String internalTransactionId;
    private String externalTransactionId;
    private String message;
    private DateTime messageTimeStamp;
    private Amount amount;
    private BigDecimal amountChips;
    private String obscuredCreditCardNumber;
    private String cashierName;
    private String gameType;
    private BigDecimal playerId;
    private BigDecimal sessionId;
    private Long promotionId;
    private Platform platform;
    private String paymentOptionId;
    private BigDecimal exchangeRate;
    private Amount baseCurrencyAmount;
    private String failureReason;

    ExternalTransactionBuilder(final ExternalTransaction externalTransaction) {
        notNull(externalTransaction, "externalTransaction may not be null");

        this.status = externalTransaction.getStatus();
        this.type = externalTransaction.getType();
        this.accountId = externalTransaction.getAccountId();
        this.internalTransactionId = externalTransaction.getInternalTransactionId();
        this.externalTransactionId = externalTransaction.getExternalTransactionId();
        this.message = externalTransaction.getCreditCardObscuredMessage();
        this.messageTimeStamp = externalTransaction.getMessageTimeStamp();
        this.amount = externalTransaction.getAmount();
        this.amountChips = externalTransaction.getAmountChips();
        this.obscuredCreditCardNumber = externalTransaction.getObscuredCreditCardNumber();
        this.cashierName = externalTransaction.getCashierName();
        this.gameType = externalTransaction.getGameType();
        this.playerId = externalTransaction.getPlayerId();
        this.sessionId = externalTransaction.getSessionId();
        this.promotionId = externalTransaction.getPromoId();
        this.platform = externalTransaction.getPlatform();
        this.paymentOptionId = externalTransaction.getPaymentOptionId();
        this.exchangeRate = externalTransaction.getExchangeRate();
        this.baseCurrencyAmount = externalTransaction.getBaseCurrencyAmount();
        this.failureReason = externalTransaction.getFailureReason();
    }

    ExternalTransactionBuilder(final BigDecimal accountId) {
        notNull(accountId, "accountId may not be null");
        this.accountId = accountId;
    }

    public ExternalTransactionBuilder withInternalTransactionId(final String newInternalTransactionId) {
        this.internalTransactionId = newInternalTransactionId;
        return this;
    }

    public ExternalTransactionBuilder withExternalTransactionId(final String newExternalTransactionId) {
        this.externalTransactionId = newExternalTransactionId;
        return this;
    }

    public ExternalTransactionBuilder withMessage(final String newMessage,
                                                  final DateTime newMessageTimeStamp) {
        if (message != null) {
            this.message = creditCardPattern.matcher(newMessage).replaceAll("$1XXXXXXXX$2");
        } else {
            this.message = newMessage;
        }
        this.messageTimeStamp = newMessageTimeStamp;
        return this;
    }

    public ExternalTransactionBuilder withAmount(final Currency newCurrency,
                                                 final BigDecimal newAmountCash) {
        this.amount = new Amount(newCurrency, newAmountCash);
        return this;
    }

    public ExternalTransactionBuilder withPaymentOption(final BigDecimal newAmountChips,
                                                        final String newPaymentOptionId) {
        this.amountChips = newAmountChips;
        this.paymentOptionId = newPaymentOptionId;
        return this;
    }

    public ExternalTransactionBuilder withCreditCardNumber(final String newCreditCardNumber) {
        this.obscuredCreditCardNumber = obscureCardNumber(newCreditCardNumber);
        return this;
    }

    public ExternalTransactionBuilder withCashierName(final String newCashierName) {
        this.cashierName = newCashierName;
        return this;
    }

    public ExternalTransactionBuilder withGameType(final String newGameType) {
        this.gameType = newGameType;
        return this;
    }

    public ExternalTransactionBuilder withStatus(final ExternalTransactionStatus newStatus) {
        this.status = newStatus;
        return this;
    }

    public ExternalTransactionBuilder withType(final ExternalTransactionType newType) {
        this.type = newType;
        return this;
    }

    public ExternalTransactionBuilder withPlayerId(final BigDecimal newPlayerId) {
        this.playerId = newPlayerId;
        return this;
    }

    public ExternalTransactionBuilder withSessionId(final BigDecimal newSessionId) {
        this.sessionId = newSessionId;
        return this;
    }

    public ExternalTransactionBuilder withPlatform(final Platform newPlatform) {
        this.platform = newPlatform;
        return this;
    }

    public ExternalTransactionBuilder withPromotionId(final Long newPromotionId) {
        this.promotionId = newPromotionId;
        return this;
    }

    public ExternalTransactionBuilder withFailureReason(final String newFailureReason) {
        this.failureReason = newFailureReason;
        return this;
    }

    public ExternalTransactionBuilder withForeignExchange(final Currency newBaseCurrency,
                                                          final BigDecimal newAmountInBaseCurrency,
                                                          final BigDecimal newExchangeRate) {
        if (newBaseCurrency != null && newAmountInBaseCurrency != null) {
            this.baseCurrencyAmount = new Amount(newBaseCurrency, newAmountInBaseCurrency);
        }
        this.exchangeRate = newExchangeRate;
        return this;
    }

    public ExternalTransaction build() {
        return new ExternalTransaction(accountId,
                internalTransactionId,
                externalTransactionId,
                message,
                messageTimeStamp,
                amount,
                amountChips,
                obscuredCreditCardNumber,
                cashierName,
                status,
                type,
                gameType,
                playerId,
                sessionId,
                promotionId,
                platform,
                paymentOptionId,
                exchangeRate,
                baseCurrencyAmount,
                failureReason);
    }

    private String obscureCardNumber(final String acctNumber) {
        if (acctNumber == null) {
            return null;
        }

        final int obscuredDigits = acctNumber.length() - CARD_DIGITS_TO_RETAIN * 2;
        if (obscuredDigits > 0) {
            return acctNumber.substring(0, CARD_DIGITS_TO_RETAIN)
                    + StringUtils.repeat(NUMBER_OBSCURED_CHARACTER, obscuredDigits)
                    + acctNumber.substring(CARD_DIGITS_TO_RETAIN + obscuredDigits);
        }

        return acctNumber;
    }
}
