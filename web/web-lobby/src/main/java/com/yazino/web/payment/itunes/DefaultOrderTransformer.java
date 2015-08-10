package com.yazino.web.payment.itunes;

import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.account.ExternalTransactionType;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.payment.PaymentState;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.time.SystemTimeSource;
import com.yazino.game.api.time.TimeSource;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.concurrent.atomic.AtomicLong;

import static com.yazino.platform.Platform.IOS;

/**
 * Default implementation or an {@link OrderTransformer}.
 * Note that on transformation, this object will only consider orders with a state of Started to be successful.
 */
class DefaultOrderTransformer<T extends Order> implements OrderTransformer<T> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss");
    private static final String OBSCURED_CREDIT_CARD_NUMBER = "x-x-x";
    private static AtomicLong atomicLong = new AtomicLong();

    private final PlayerService mPlayerService;

    private TimeSource mTimeSource = new SystemTimeSource();

    @Autowired
    public DefaultOrderTransformer(final PlayerService playerService) {
        Validate.notNull(playerService);
        mPlayerService = playerService;
    }

    @Override
    public ExternalTransaction transform(final T order) {
        final BigDecimal accountId = mPlayerService.getAccountId(order.getPlayerId());
        final DateTime messageTimeStamp = new DateTime(mTimeSource.getCurrentTimeStamp());
        final String internalIdentifier = buildInternalId(messageTimeStamp, accountId, order);
        final String externalIdentifier = order.getOrderId();
        final String creditCardObscuredMessage = order.getMessage();
        final Currency currency = order.getCurrency();
        final BigDecimal cashAmount = order.getCashAmount();
        final BigDecimal chipAmount = order.getChipAmount();
        final String cashier = order.getCashier();
        final ExternalTransactionStatus status = lookupTransactionStatus(order.getPaymentState());
        final ExternalTransactionType type = ExternalTransactionType.DEPOSIT;
        final String gameType = order.getGameType();
        Long promoId = populatePromoId(order.getPaymentOption());
        return ExternalTransaction.newExternalTransaction(accountId)
                .withInternalTransactionId(internalIdentifier)
                .withExternalTransactionId(externalIdentifier)
                .withMessage(creditCardObscuredMessage, messageTimeStamp)
                .withAmount(currency, cashAmount)
                .withPaymentOption(chipAmount, null)
                .withCreditCardNumber(OBSCURED_CREDIT_CARD_NUMBER)
                .withCashierName(cashier)
                .withStatus(status)
                .withType(type)
                .withGameType(gameType)
                .withPlayerId(order.getPlayerId())
                .withPromotionId(promoId)
                .withPlatform(IOS)
                .build();
    }

    private Long populatePromoId(final PaymentOption paymentOption) {
        final PromotionPaymentOption promotion =paymentOption.getPromotion(PaymentPreferences.PaymentMethod.ITUNES);
        if (promotion!=null)
        {
            return promotion.getPromoId();
        }
        return null;
    }

    private ExternalTransactionStatus lookupTransactionStatus(final PaymentState state) {
        if (state == PaymentState.Started) {
            return ExternalTransactionStatus.SUCCESS;
        }
        return ExternalTransactionStatus.FAILURE;
    }

    String buildInternalId(final DateTime messageTimeStamp,
                           final BigDecimal accountId,
                           final Order order) {
        final DateTime dtLondon = messageTimeStamp.withZone(DateTimeZone.forID("Europe/London"));
        final String date = DATE_TIME_FORMATTER.print(dtLondon);
        final long nextIncrement = atomicLong.getAndIncrement();
        return String.format("%s_%s_%s_%s_%s", order.getCashier(), order.getProductId(),
                accountId.toPlainString(), date, nextIncrement);
    }

    public void setTimeSource(final TimeSource timeSource) {
        Validate.notNull(timeSource);
        mTimeSource = timeSource;
    }

    static void setAtomicLong(final AtomicLong aLong) {
        // testing only
        atomicLong = aLong;
    }

}
