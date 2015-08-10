package com.yazino.web.payment.amazon;

import com.yazino.platform.Partner;
import com.yazino.web.domain.PaymentEmailBodyTemplate;
import com.yazino.web.domain.email.EmailBuilder;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.googlecheckout.VerifiedOrder;
import com.yazino.web.payment.googlecheckout.VerifiedOrderBuilder;
import com.yazino.web.service.QuietPlayerEmailer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

public class PlayerNotifierTest {
    public static final String GAME_TYPE = "SLOTS";
    private static final BigDecimal CHIPS = TEN;
    private static final BigDecimal PRICE = ONE;
    private static final String CURRENCY = "USD";
    public static final String PRODUCT_ID = "product-id";
    private static final String ORDER_ID = "order-data";
    private static final BigDecimal PLAYER_ID = ONE;
    private static final String PLAYER_NAME = "jack";
    private static final String PLAYER_EMAIL = "jack@here.com";
    public static final BigDecimal ACCOUNT_ID = ONE;

    @Mock
    private QuietPlayerEmailer emailer;

    private PlayerNotifier underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new PlayerNotifier(emailer);
    }

    @Test
    public void shouldNotifyPlayer() {
        underTest.emailPlayer(buildPaymentContext(), buildVerifiedOrder(), PaymentEmailBodyTemplate.Amazon);

        verify(emailer).quietlySendEmail(any(EmailBuilder.class));
    }

    private PaymentContext buildPaymentContext() {
        return new PaymentContext(PLAYER_ID, null, PLAYER_NAME, GAME_TYPE, PLAYER_EMAIL, null, null, Partner.YAZINO);
    }

    private VerifiedOrder buildVerifiedOrder() {
        return new VerifiedOrderBuilder().withOrderId(ORDER_ID).withProductId(PRODUCT_ID).withCurrencyCode(CURRENCY)
                .withPrice(PRICE).withChips(CHIPS).buildVerifiedOrder();
    }
}
