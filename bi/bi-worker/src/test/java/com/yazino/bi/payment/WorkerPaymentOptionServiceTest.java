package com.yazino.bi.payment;

import com.google.common.base.Optional;
import com.yazino.bi.payment.persistence.JDBCPaymentOptionDAO;
import com.yazino.platform.Platform;
import com.yazino.platform.reference.Currency;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkerPaymentOptionServiceTest {
    @Mock
    private JDBCPaymentOptionDAO paymentOptionDao;

    private WorkerPaymentOptionService underTest;

    @Before
    public void init() {
        underTest = new WorkerPaymentOptionService(paymentOptionDao);
    }

    @Test
    public void aPaymentOptionCanBeFoundByIdAndPlatform() {
        when(paymentOptionDao.findByIdAndPlatform("anOptionId", Platform.ANDROID))
                .thenReturn(fromNullable(aPaymentOption("anOptionId")));

        final PaymentOption paymentOption = underTest.getDefault("anOptionId", Platform.ANDROID);

        assertThat(paymentOption, is(equalTo(aPaymentOption("anOptionId"))));
    }

    @Test
    public void findingAPaymentOptionThatDoesNotExistReturnsNull() {
        when(paymentOptionDao.findByIdAndPlatform("anOptionId", Platform.ANDROID)).thenReturn(Optional.<PaymentOption>absent());

        final PaymentOption paymentOption = underTest.getDefault("anOptionId", Platform.ANDROID);

        assertThat(paymentOption, is(nullValue()));
    }


    @Test
    public void gettingAllPaymentOptionsByCurrencyAndPlatformDelegatesToTheDAO() {
        when(paymentOptionDao.findByCurrencyAndPlatform(Currency.UAH, Platform.WEB))
                .thenReturn(asList(aPaymentOption("option1"), aPaymentOption("option2")));

        final Collection<PaymentOption> paymentOptions = underTest.getAllPaymentOptions(Currency.UAH, Platform.WEB);

        assertThat(paymentOptions, contains(aPaymentOption("option1"), aPaymentOption("option2")));
    }

    @Test
    public void getAllPaymentOptionsForWeb() {
        final PaymentOption expectedPaymentOption = new PaymentOptionBuilder().setId("webId").createPaymentOption();
        final Set<PaymentOption> expectedPaymentOptions = newHashSet(expectedPaymentOption);

        when(paymentOptionDao.findByPlatform(Platform.WEB)).thenReturn(expectedPaymentOptions);

        final Collection<PaymentOption> actualPaymentOptions = underTest.getAllPaymentOptions(Platform.WEB);

        assertThat(actualPaymentOptions, hasItems(expectedPaymentOption));
        assertEquals(actualPaymentOptions.size(), 1);

    }

    @Test
    public void getAllPaymentOptionsForIosShouldReturnDefaultIOSChipsPaymentOptions() {
        final PaymentOption expectedPaymentOption = new PaymentOptionBuilder().setId("IOSId").createPaymentOption();
        final Set<PaymentOption> expectedPaymentOptions = newHashSet(expectedPaymentOption);

        when(paymentOptionDao.findByPlatform(Platform.IOS)).thenReturn(expectedPaymentOptions);

        final Collection<PaymentOption> actualPaymentOptions = underTest.getAllPaymentOptions(Platform.IOS);

        assertThat(actualPaymentOptions, hasItems(expectedPaymentOption));
        assertEquals(actualPaymentOptions.size(), 1);
    }

    @Test
    public void getAllPaymentOptionsForFacebookshouldReturnDefaultIOSChipsPaymentOptions() {
        final PaymentOption expectedPaymentOption = new PaymentOptionBuilder().setId("facebookId").createPaymentOption();
        final Set<PaymentOption> expectedPaymentOptions = newHashSet(expectedPaymentOption);

        when(paymentOptionDao.findByPlatform(Platform.FACEBOOK_CANVAS)).thenReturn(expectedPaymentOptions);

        final Collection<PaymentOption> actualPaymentOptions = underTest.getAllPaymentOptions(Platform.FACEBOOK_CANVAS);

        assertThat(actualPaymentOptions, hasItems(expectedPaymentOption));
        assertEquals(actualPaymentOptions.size(), 1);
    }

    @Test
    public void getAllPaymentOptionsForAndroidShouldReturnDefaultAndroidChipsPaymentOptions() {
        final PaymentOption expectedPaymentOption = new PaymentOptionBuilder().setId("androidId").createPaymentOption();
        final Set<PaymentOption> expectedPaymentOptions = newHashSet(expectedPaymentOption);

        when(paymentOptionDao.findByPlatform(Platform.ANDROID)).thenReturn(expectedPaymentOptions);

        final Collection<PaymentOption> actualPaymentOptions = underTest.getAllPaymentOptions(Platform.ANDROID);

        assertThat(actualPaymentOptions, hasItems(expectedPaymentOption));
        assertEquals(actualPaymentOptions.size(), 1);
    }

    private PaymentOption aPaymentOption(final String id) {
        final PaymentOption paymentOption = new PaymentOption();
        paymentOption.setId(id);
        return paymentOption;
    }
}
