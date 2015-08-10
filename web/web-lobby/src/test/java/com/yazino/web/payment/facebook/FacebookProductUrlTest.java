package com.yazino.web.payment.facebook;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PaymentPreferences;
import org.junit.Assert;
import org.junit.Test;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FacebookProductUrlTest {

    @Test
    public void validUrlShouldCreateProduct() throws WalletServiceException {
        final FacebookProductUrl usd3 = new FacebookProductUrl(
                "http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_10_buys_100k");
        assertThat(usd3.getPath(), is(equalTo("http://env-proxy.london.yazino.com/jrae-centos/fbog/product/")));

        assertThat(usd3.getPackage(), is(equalTo("usd3")));
        assertThat(usd3.getWebPackage(), is(equalTo("optionUSD3")));
        assertThat(usd3.getCashValue(),comparesEqualTo(BigDecimal.TEN));
    }

    @Test
    public void validPromoUrlShouldCreatePromoProduct() throws WalletServiceException {
        final FacebookProductUrl usd3 = new FacebookProductUrl(
                "http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_10_buys_100k_8");
        assertThat(usd3.getPath(), is(equalTo("http://env-proxy.london.yazino.com/jrae-centos/fbog/product/")));
        assertThat(usd3.getPackage(), is(equalTo("usd3")));
        assertThat(usd3.getWebPackage(), is(equalTo("optionUSD3")));
        assertThat(usd3.getCashValue(), comparesEqualTo(BigDecimal.TEN));
        assertThat(usd3.getPromoId(), is(equalTo(8l)));
    }

    @Test
    public void packageUrlShouldBeRebuiltFromLoadedPromo() throws WalletServiceException {
        final FacebookProductUrl incoming = new FacebookProductUrl(
                        "http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_10_buys_500k_8");
        final PaymentOption paymentOption= mock(PaymentOption.class);
        when(paymentOption.getNumChipsPerPurchase("FACEBOOK")).thenReturn(BigDecimal.valueOf(200000));
        when(paymentOption.getAmountRealMoneyPerPurchase()).thenReturn(BigDecimal.valueOf(20));
        when(paymentOption.getRealMoneyCurrency()).thenReturn("USD");
        final PromotionPaymentOption promo=mock(PromotionPaymentOption.class);
        when(promo.getPromoId()).thenReturn(8l);
        when(paymentOption.getPromotion(PaymentPreferences.PaymentMethod.FACEBOOK)).thenReturn(promo);

        final String rebuiltUrl = new FacebookProductUrl(paymentOption, incoming).toUrl();
        Assert.assertThat(rebuiltUrl, equalTo("http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_20_buys_200k_8"));
    }

    @Test(expected = WalletServiceException.class)
    public void MissingPathShouldThrowException() throws WalletServiceException {
        new FacebookProductUrl("/fbog/product/usd3_10_buys_100k_8");

    }

    @Test(expected = WalletServiceException.class)
    public void MissingKeyShouldThrowException() throws WalletServiceException {
        new FacebookProductUrl("http://env-proxy.london.yazino.com/jrae-centos/product/usd3_10_buys_100k_8");

    }

    @Test(expected = WalletServiceException.class)
    public void MissingCashShouldThrowException() throws WalletServiceException {
        new FacebookProductUrl("http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_buys_100k_8");

    }

    @Test(expected = WalletServiceException.class)
    public void MissingBuysShouldThrowException() throws WalletServiceException {
        new FacebookProductUrl("http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_20_100k_8");

    }

    @Test(expected = WalletServiceException.class)
    public void MissingChipsShouldThrowException() throws WalletServiceException {
        new FacebookProductUrl("http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_20_buys_8");

    }

    @Test(expected = WalletServiceException.class)
    public void MissingPackageShouldThrowException() throws WalletServiceException {
        new FacebookProductUrl("http://env-proxy.london.yazino.com/jrae-centos/fbog/product/20_buys_100k");

    }

    @Test(expected = WalletServiceException.class)
    public void InvalidPackageShouldThrowException() throws WalletServiceException {
        new FacebookProductUrl("http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd_20_buys_100k");

    }

    @Test(expected = WalletServiceException.class)
    public void ExtraFieldShouldThrowException() throws WalletServiceException {
        new FacebookProductUrl("http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_20_buys_100k_8_9");

    }
}
