package com.yazino.bi.payment;

import com.yazino.bi.payment.worldpay.WorldPayChargebackUpdater;
import com.yazino.bi.payment.worldpay.WorldPayForexUpdater;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PaymentControllerTest {
    @Mock
    private WorldPayForexUpdater forexUpdater;
    @Mock
    private WorldPayChargebackUpdater chargebackUpdater;
    @Mock
    private HttpServletResponse response;

    private PaymentController underTest;

    @Before
    public void setUp() {
        underTest = new PaymentController(forexUpdater, chargebackUpdater);
    }

    @Test
    public void updatingExchangeRatesCallsTheForexUpdater() {
        underTest.updateExchangeRates(response);

        verify(forexUpdater).updateExchangeRates();
    }

    @Test
    public void updatingExchangeRatesReturnsOK() {
        underTest.updateExchangeRates(response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    public void updatingChargebacksCallsTheChargebackUpdaterForYesterday() {
        underTest.updateChargebacks(response);

        verify(chargebackUpdater).updateChargebacksForYesterday();
    }

    @Test
    public void updatingChargebacksForDateCallsTheChargebackUpdaterForTheGivenDate() {
        underTest.updateChargebacksFor(response, "20130203");

        verify(chargebackUpdater).updateChargebacksFor(new DateTime(2013, 2, 3, 0, 0, 0));
    }

    @Test
    public void updatingChargebacksReturnsOK() {
        underTest.updateChargebacks(response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    public void updatingChargebacksForDateReturnsOK() {
        underTest.updateChargebacksFor(response, "20130101");

        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    public void updatingChargebacksForDateReturnsBadRequestWhenDateIsInvalid() {
        underTest.updateChargebacksFor(response, "201301xx");

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

}
