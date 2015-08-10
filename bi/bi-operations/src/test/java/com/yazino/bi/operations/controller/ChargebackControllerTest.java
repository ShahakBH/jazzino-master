package com.yazino.bi.operations.controller;

import com.yazino.bi.payment.Chargeback;
import com.yazino.bi.payment.persistence.JDBCPaymentChargebackDAO;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.model.PagedData;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class ChargebackControllerTest {
    private static final DateTime START_DATE = new DateTime(2013, 10, 1, 0, 0, 0);
    private static final DateTime END_DATE = new DateTime(2013, 10, 3, 0, 0, 0);
    private static final boolean ONLY_CHALLENGE_REASONS = true;
    @Mock
    private JDBCPaymentChargebackDAO chargebackDao;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private ChargebackController underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2013, 10, 1, 0, 0, 0).getMillis());

        underTest = new ChargebackController(chargebackDao, yazinoConfiguration);

        when(yazinoConfiguration.getStringArray("payment.worldpay.chargeback.challenge")).thenReturn(new String[]{"30", "40", "50"});
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void queryingWithAPageNumberPassesThePageNumberAndPageSizeToTheDAO() {
        underTest.chargebacks(aForm(), 3);

        verify(chargebackDao).search(any(DateTime.class), any(DateTime.class), anyList(), eq(2), eq(20));
    }

    @Test
    public void queryingWithoutAPageNumberPassesPageOneAndThePageSizeToTheDAO() {
        underTest.chargebacks(aForm());

        verify(chargebackDao).search(any(DateTime.class), any(DateTime.class), anyList(), eq(0), eq(20));
    }

    @Test
    public void queryingWithOnlyChallengeableResponsesSetsTheConfiguredCodesInTheQuery() {
        underTest.chargebacks(aForm());

        verify(chargebackDao).search(any(DateTime.class), any(DateTime.class), eq(asList("30", "40", "50")), anyInt(), anyInt());
    }

    @Test
    public void queryingWithAllResponsesPassesNullToTheQuery() {
        final ChargebackForm form = aForm();
        form.setOnlyChallengeReasons(false);

        underTest.chargebacks(form);

        verify(chargebackDao).search(any(DateTime.class), any(DateTime.class), (List<String>) Matchers.isNull(), anyInt(), anyInt());
    }

    @Test
    public void queryingPassesTheStartAndEndDatesToTheQuery() {
        underTest.chargebacks(aForm(), 3);

        verify(chargebackDao).search(eq(START_DATE), eq(END_DATE), anyList(), anyInt(), anyInt());
    }

    @Test
    public void aDefaultFormUsesTheLastWeekAsTheDatePeriod() {
        final DateTime now = new DateTime();
        underTest.chargebacks(new ChargebackForm(), 3);

        verify(chargebackDao).search(eq(now.minusDays(7)), eq(now), (List<String>) isNull(), anyInt(), anyInt());
    }

    @Test
    public void queriesAreReturnedToTheChargebackView() {
        final ModelAndView modelAndView = underTest.chargebacks(aForm(), 3);

        assertThat(modelAndView.getViewName(), is(equalTo("payments/chargebacks")));
    }

    @Test
    public void queriesHaveSearchResultsReturnedInTheModel() {
        final PagedData<Chargeback> chargebacks = new PagedData<>(100, 10, 1000, Collections.<Chargeback>emptyList());
        when(chargebackDao.search(any(DateTime.class), any(DateTime.class), anyList(), anyInt(), anyInt())).thenReturn(chargebacks);

        final ModelAndView modelAndView = underTest.chargebacks(aForm(), 3);

        assertThat((PagedData<Chargeback>) modelAndView.getModel().get("chargebacks"), is(equalTo(chargebacks)));
    }

    @Test
    public void queriesHaveTheFormReturnedInTheModel() {
        final ModelAndView modelAndView = underTest.chargebacks(aForm(), 3);

        assertThat((ChargebackForm) modelAndView.getModel().get("form"), is(equalTo(aForm())));
    }

    private ChargebackForm aForm() {
        return new ChargebackForm(START_DATE.toDate(), END_DATE.toDate(), ONLY_CHALLENGE_REASONS);
    }
}
