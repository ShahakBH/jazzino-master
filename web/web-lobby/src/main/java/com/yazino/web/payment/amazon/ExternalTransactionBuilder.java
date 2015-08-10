package com.yazino.web.payment.amazon;

import com.google.common.base.Objects;
import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.community.PlayerService;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.Purchase;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;

import static com.yazino.platform.account.ExternalTransactionType.DEPOSIT;
import static java.math.BigDecimal.ZERO;

@Component
public class ExternalTransactionBuilder {
    public static final String CURRENCY_USD = "USD";
    private static final String OBSCURED_CREDIT_CARD_NUMBER = "none";

    private PlayerService playerService;

    @Autowired
    public ExternalTransactionBuilder(final PlayerService playerService) {
        this.playerService = playerService;
    }

    public ExternalTransaction build(final String cashierName,
                                     final Platform platform,
                                     final ExternalTransactionStatus txnStatus,
                                     final PaymentContext paymentContext,
                                     final Long promoId,
                                     final Purchase purchase) {

        final BigDecimal accountId = playerService.getAccountId(paymentContext.getPlayerId());

        return ExternalTransaction.newExternalTransaction(accountId)
                .withInternalTransactionId(purchase.getPurchaseId())
                .withExternalTransactionId(purchase.getExternalId())
                .withMessage(purchase.getProductId(), new DateTime())
                .withAmount(Currency.getInstance(Objects.firstNonNull(purchase.getCurrencyCode(), CURRENCY_USD)),
                            Objects.firstNonNull(purchase.getPrice(), ZERO))
                .withPaymentOption(Objects.firstNonNull(purchase.getChips(), ZERO), null)
                .withCreditCardNumber(OBSCURED_CREDIT_CARD_NUMBER)
                .withCashierName(cashierName)
                .withStatus(txnStatus)
                .withType(DEPOSIT)
                .withGameType(paymentContext.getGameType())
                .withPlayerId(paymentContext.getPlayerId())
                .withPromotionId(promoId)
                .withPlatform(platform)
                .build();
    }
}
