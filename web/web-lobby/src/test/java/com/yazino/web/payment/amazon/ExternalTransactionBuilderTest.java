package com.yazino.web.payment.amazon;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.community.PlayerService;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.Purchase;
import com.yazino.web.payment.googlecheckout.VerifiedOrder;
import com.yazino.web.payment.googlecheckout.VerifiedOrderBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static com.yazino.platform.Platform.AMAZON;
import static com.yazino.platform.account.ExternalTransactionStatus.SUCCESS;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class ExternalTransactionBuilderTest {
    public static final Platform PLATFORM = AMAZON;
    public static final String GAME_TYPE = "SLOTS";
    private static final BigDecimal CHIPS = TEN;
    private static final BigDecimal PRICE = ONE;
    private static final String CURRENCY = "USD";
    public static final String PRODUCT_ID = "product-id";
    private static final String INTERNAL_ID = "order-data";
    private static final BigDecimal PLAYER_ID = ONE;
    private static final String PLAYER_NAME = "jack";
    private static final String PLAYER_EMAIL = "jack@here.com";
    private static final String MESSAGE = "message";
    private static final String CASHIER_NAME = "Amazon";
    private static final Partner PARTNER= Partner.YAZINO;
    public static final String EXTERNAL_ID = "external Id";
    @Mock
    private PlayerService playerService;

    private ExternalTransactionBuilder underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new ExternalTransactionBuilder(playerService);
    }

    @Test
    public void shouldBuildExternalTransactionForPurchase() {
         when(playerService.getAccountId(PLAYER_ID)).thenReturn(BigDecimal.valueOf(1));

         ExternalTransaction txn = underTest.build(CASHIER_NAME, PLATFORM, SUCCESS, buildPaymentContext(), null, buildPurchaseRequest());

         assertEquals(CHIPS, txn.getAmountChips());
         assertEquals(CURRENCY, txn.getCurrency().getCurrencyCode());
         assertEquals(AMAZON, txn.getPlatform());
         assertEquals(SUCCESS, txn.getStatus());
         assertEquals(INTERNAL_ID, txn.getInternalTransactionId());
         assertEquals(EXTERNAL_ID, txn.getExternalTransactionId());
         assertEquals(PRODUCT_ID, txn.getCreditCardObscuredMessage());
    }

    private PaymentContext buildPaymentContext() {
        return new PaymentContext(PLAYER_ID, null, PLAYER_NAME, GAME_TYPE, PLAYER_EMAIL, null, null, PARTNER);
    }

    private VerifiedOrder buildVerifiedOrder() {
        return new VerifiedOrderBuilder().withOrderId(INTERNAL_ID).withProductId(PRODUCT_ID).withCurrencyCode(CURRENCY)
                .withPrice(PRICE).withChips(CHIPS).buildVerifiedOrder();
    }

    private Purchase buildPurchaseRequest() {
        final Purchase purchase = new Purchase();
        purchase.setExternalId(EXTERNAL_ID);
        purchase.setPurchaseId(INTERNAL_ID);
        purchase.setCurrencyCode(CURRENCY);
        purchase.setProductId(PRODUCT_ID);
        purchase.setPrice(PRICE);
        purchase.setChips(CHIPS);

        return purchase;
    }
}
