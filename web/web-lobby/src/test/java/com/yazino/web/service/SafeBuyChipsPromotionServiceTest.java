package com.yazino.web.service;

import com.yazino.bi.payment.PaymentOptionService;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.reference.Currency;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;
import strata.server.lobby.api.promotion.InGameMessage;
import com.yazino.bi.payment.PaymentOption;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.platform.Platform.WEB;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SafeBuyChipsPromotionServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;

    @Mock
    private BuyChipsPromotionService delegate;
    @Mock
    private PaymentOptionService paymentOptionService;

    private Map<Currency, List<PaymentOption>> defaultPaymentOptions = newHashMap();
    private final RuntimeException aException = new RuntimeException("any error");
    private PaymentOption defaultPaymentOption = new PaymentOption();

    private SafeBuyChipsPromotionService underTest;

    @Before
    public void setUp() throws Exception {
        defaultPaymentOption.setId("defaultPaymentOption");

        when(paymentOptionService.getAllDefaults(any(Platform.class))).thenReturn(defaultPaymentOptions);
        when(paymentOptionService.getDefault(any(String.class), any(Platform.class))).thenReturn(defaultPaymentOption);

        defaultPaymentOptions.put(Currency.USD, new ArrayList<PaymentOption>());

        underTest = new SafeBuyChipsPromotionService(delegate, paymentOptionService);
    }

    @Test
    public void hasPromotionShouldNotFail(){
        when(delegate.hasPromotion(any(BigDecimal.class), any(Platform.class))).thenThrow(aException);
        Assert.assertThat(underTest.hasPromotion(PLAYER_ID, WEB), is(Boolean.FALSE));
    }

    @Test
    public void hasPromotionShouldReturnCorrectValue() {
        when(delegate.hasPromotion(any(BigDecimal.class), any(Platform.class))).thenReturn(Boolean.TRUE);
        Assert.assertThat(underTest.hasPromotion(PLAYER_ID, WEB), is(true));
    }

    @Test
    public void getBuyChipsPaymentOptionsForShouldNotFail() {
        when(delegate.getBuyChipsPaymentOptionsFor(any(BigDecimal.class), any(Platform.class))).thenThrow(aException);
        Assert.assertThat(underTest.getBuyChipsPaymentOptionsFor(PLAYER_ID, WEB), is(equalTo(defaultPaymentOptions)));
    }

    @Test
    public void getBuyChipsPaymentOptionsForShouldReturnCorrectValue() {
        final Map<Currency, List<PaymentOption>> loadedPaymentOptions = newHashMap();
        when(delegate.getBuyChipsPaymentOptionsFor(any(BigDecimal.class), any(Platform.class))).thenReturn(loadedPaymentOptions);
        Assert.assertThat(underTest.getBuyChipsPaymentOptionsFor(PLAYER_ID, WEB), is(equalTo(loadedPaymentOptions)));
    }

    @Test
    public void getInGameMessageForShouldNotFail() {
        when(delegate.getInGameMessageFor(any(BigDecimal.class))).thenThrow(aException);
        assertNull(underTest.getInGameMessageFor(PLAYER_ID));
    }

    @Test
    public void getInGameMessageForReturnsCorrectValue() {
        final InGameMessage expected = new InGameMessage("aHeader", "aMsg");
        when(delegate.getInGameMessageFor(any(BigDecimal.class))).thenReturn(expected);
        assertEquals(expected, underTest.getInGameMessageFor(PLAYER_ID));
    }

    @Test
    public void getPaymentOptionForShouldNotFail() {
        when(delegate.getPaymentOptionFor(any(BigDecimal.class), any(Long.class), any(PaymentPreferences.PaymentMethod.class), any(String.class))).thenThrow(aException);
        assertNull(underTest.getPaymentOptionFor(PLAYER_ID, 1L, CREDITCARD, "anything"));
    }

    @Test
    public void getPaymentOptionForReturnsCorrectValue() {
        final PaymentOption expected = new PaymentOption();
        expected.setId("aPaymentId");
        when(delegate.getPaymentOptionFor(any(BigDecimal.class), any(Long.class), any(PaymentPreferences.PaymentMethod.class), any(String.class))).thenReturn(expected);
        assertEquals(expected, underTest.getPaymentOptionFor(PLAYER_ID, 1L, CREDITCARD, "anything"));
    }

    @Test
    public void getDefaultPaymentOptionForShouldNotFail() {
        when(delegate.getDefaultPaymentOptionFor(any(String.class), any(Platform.class))).thenThrow(aException);
        assertEquals(defaultPaymentOption, underTest.getDefaultPaymentOptionFor("aPaymentId", WEB));
    }

    @Test
    public void getDefaultPaymentOptionForReturnsCorrectValue() {
        final PaymentOption expected = new PaymentOption();
        expected.setId("aPaymentId");
        when(delegate.getDefaultPaymentOptionFor(any(String.class), any(Platform.class))).thenReturn(expected);
        assertEquals(expected, underTest.getDefaultPaymentOptionFor("aPaymentId", WEB));
    }

    @Test
    public void logPlayerRewardShouldNotFail() {
        doThrow(aException).when(delegate).logPlayerReward(any(BigDecimal.class), any(Long.class), any(PaymentPreferences.PaymentMethod.class), any(String.class), any(DateTime.class));
        underTest.logPlayerReward(PLAYER_ID, 1L, PAYPAL, "optionId", new DateTime());
    }

    @Test
    public void logPlayerRewardForGivenChipsShouldNotFail() {
        doThrow(aException).when(delegate).logPlayerReward(any(BigDecimal.class), any(Long.class), any(BigDecimal.class), any(BigDecimal.class), any(PaymentPreferences.PaymentMethod.class), any(DateTime.class));
        underTest.logPlayerReward(PLAYER_ID, 1L, BigDecimal.ONE, BigDecimal.TEN, GOOGLE_CHECKOUT, new DateTime());
    }
}
