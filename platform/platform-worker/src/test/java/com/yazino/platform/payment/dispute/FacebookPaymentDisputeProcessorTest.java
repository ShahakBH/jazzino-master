package com.yazino.platform.payment.dispute;

import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransactionType;
import com.yazino.platform.payment.DisputeResolution;
import com.yazino.platform.payment.PaymentDispute;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Currency;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FacebookPaymentDisputeProcessorTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(-1000).setScale(2);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(-100).setScale(2);
    private static final BigDecimal PRICE = BigDecimal.valueOf(200).setScale(4);
    private static final BigDecimal CHIPS = BigDecimal.valueOf(2100).setScale(4);
    private static final Long PROMOTION_ID = 3141592L;
    private static final DateTime BASE_DATE = new DateTime(2013, 2, 5, 10, 15);

    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private FacebookClientFactory facebookClientFactory;
    @Mock
    private FacebookClient facebookClient;

    private FacebookPaymentDisputeProcessor underTest;

    @Before
    public void setUp() {
        when(yazinoConfiguration.getString("facebook.clientAccessToken.aGameType")).thenReturn("aToken");
        when(facebookClientFactory.facebookClientFor("aToken")).thenReturn(facebookClient);

        underTest = new FacebookPaymentDisputeProcessor(yazinoConfiguration, facebookClientFactory);
    }

    @Test(expected = NullPointerException.class)
    public void raisingANullDisputeCausesAnException() {
        underTest.raise(null);
    }

    @Test
    public void raisingADisputeDoesNothing() {
        underTest.raise(anOpenDispute(1));

        verifyZeroInteractions(yazinoConfiguration, facebookClientFactory, facebookClient);
    }

    @Test(expected = NullPointerException.class)
    public void resolvingANullDisputeCausesAnException() {
        underTest.resolve(null);
    }

    @Test(expected = IllegalStateException.class)
    public void resolvingADisputeWithNoResolutionCausesAnIllegalStateException() {
        underTest.resolve(anOpenDispute(1));
    }

    @Test(expected = IllegalStateException.class)
    public void resolvingADisputeWithNoGameTypeCausesAnIllegalStateException() {
        underTest.resolve(PaymentDispute.copy(aResolvedDispute(1, DisputeResolution.REFUNDED_FRAUD)).withGameType(null).build());
    }

    @Test
    public void resolvingADisputeAsRefusedResolvesWithAnOutcomeOfDeniedRefund() {
        underTest.resolve(aResolvedDispute(1, DisputeResolution.REFUSED));

        verify(facebookClient).publish("anExternalTransactionId/dispute", Boolean.class, Parameter.with("reason", "DENIED_REFUND"));
        verifyNoMoreInteractions(facebookClient);
    }

    @Test
    public void resolvingADisputeAsRefusedBannedResolvesWithAnOutcomeOfBannedUser() {
        underTest.resolve(aResolvedDispute(1, DisputeResolution.REFUSED_BANNED));

        verify(facebookClient).publish("anExternalTransactionId/dispute", Boolean.class, Parameter.with("reason", "BANNED_USER"));
        verifyNoMoreInteractions(facebookClient);
    }

    @Test
    public void resolvingADisputeAsChipsCreditedResolvesWithAnOutcomeOfGrantedReplacementItem() {
        underTest.resolve(aResolvedDispute(1, DisputeResolution.CHIPS_CREDITED));

        verify(facebookClient).publish("anExternalTransactionId/dispute", Boolean.class, Parameter.with("reason", "GRANTED_REPLACEMENT_ITEM"));
        verifyNoMoreInteractions(facebookClient);
    }

    @Test
    public void resolvingADisputeAsRefundedFraudRefundsWithAnOutcomeOfMaliciousFraud() {
        underTest.resolve(aResolvedDispute(1, DisputeResolution.REFUNDED_FRAUD));

        verify(facebookClient).publish("anExternalTransactionId/refunds", Boolean.class,
                Parameter.with("currency", "GBP"),
                Parameter.with("amount", PRICE),
                Parameter.with("reason", "MALICIOUS_FRAUD"));
        verifyNoMoreInteractions(facebookClient);
    }

    @Test
    public void resolvingADisputeAsRefundedPlayerErrorRefundsWithAnOutcomeOfFriendlyFraud() {
        underTest.resolve(aResolvedDispute(1, DisputeResolution.REFUNDED_PLAYER_ERROR));

        verify(facebookClient).publish("anExternalTransactionId/refunds", Boolean.class,
                Parameter.with("currency", "GBP"),
                Parameter.with("amount", PRICE),
                Parameter.with("reason", "FRIENDLY_FRAUD"));
        verifyNoMoreInteractions(facebookClient);
    }

    @Test
    public void resolvingADisputeAsRefundedOtherRefundsWithAnOutcomeOfCustomerService() {
        underTest.resolve(aResolvedDispute(1, DisputeResolution.REFUNDED_OTHER));

        verify(facebookClient).publish("anExternalTransactionId/refunds", Boolean.class,
                Parameter.with("currency", "GBP"),
                Parameter.with("amount", PRICE),
                Parameter.with("reason", "CUSTOMER_SERVICE"));
        verifyNoMoreInteractions(facebookClient);
    }

    private PaymentDispute anOpenDispute(final int id) {
        return PaymentDispute.newDispute("internalTx" + id,
                "testCashier",
                "anExternalTransactionId",
                PLAYER_ID,
                ACCOUNT_ID,
                BASE_DATE.minusHours(id),
                PRICE,
                Currency.getInstance("GBP"),
                CHIPS,
                ExternalTransactionType.DEPOSIT,
                "aDisputeReason")
                .withGameType("aGameType")
                .withPlatform(Platform.WEB)
                .withPaymentOptionId("aPaymentOptionId")
                .withPromotionId(PROMOTION_ID)
                .build();
    }

    private PaymentDispute aResolvedDispute(final int id, final DisputeResolution resolution) {
        return PaymentDispute.newDispute("internalTx" + id,
                "testCashier",
                "anExternalTransactionId",
                PLAYER_ID,
                ACCOUNT_ID,
                BASE_DATE.minusHours(id),
                PRICE,
                Currency.getInstance("GBP"),
                CHIPS,
                ExternalTransactionType.DEPOSIT,
                "aDisputeReason")
                .withGameType("aGameType")
                .withPlatform(Platform.WEB)
                .withPaymentOptionId("aPaymentOptionId")
                .withPromotionId(PROMOTION_ID)
                .withResolution(resolution,
                        BASE_DATE,
                        "testResolution",
                        "aTester")
                .build();
    }
}