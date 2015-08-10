package com.yazino.bi.operations.controller;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;
import com.yazino.bi.operations.persistence.CurrencyRatesDefinition;
import com.yazino.bi.operations.persistence.PaymentTransactionReportDao;
import com.yazino.bi.operations.util.DataFormatHelper;
import com.yazino.bi.operations.model.ReportDefinitionCommand;
import com.yazino.bi.operations.view.reportbeans.PaymentReportData;
import com.yazino.bi.operations.view.reportbeans.PaymentTransactionData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static com.yazino.bi.operations.persistence.CurrencyRatesDefinition.*;

@RunWith(MockitoJUnitRunner.class)
public class PaymentTransactionControllerTest {
    @Mock
    private PaymentTransactionReportDao dao;
    @Mock
    private CurrencyRatesDefinition ratesDef;

    private PaymentTransactionController underTest;

    @Before
    public void init() {
        underTest = new PaymentTransactionController(dao, ratesDef);
    }

    // TODO RATE TEST should use command rates
    @Test
    public void shouldCreateReportForDateRange() {
        // GIVEN the controller receives a request of data per date
        final ReportDefinitionCommand command = new ReportDefinitionCommand();
        command.setFromDate("2011-06-07");
        command.setToDate("2011-06-07");
        command.setCurrencyCode("EUR");
        command.setTxnStatus("SUCCESS");
        command.setPaymentMethod("Wirecard");
        command.setPaymentTransactionReportType("all");

        final List<PaymentTransactionData> initialList = new ArrayList<PaymentTransactionData>();
        final PaymentTransactionData bean1 = new PaymentTransactionData();
        bean1.setCashier("cashier 1");
        bean1.setAmount(100D);
        bean1.setInternalId("internal");
        bean1.setPlayerId(9887l);
        bean1.setCurrencyCode("USD");
        bean1.setDetails("details");
        bean1.setDate("07/06/2011");
        bean1.setExternalId("external");
        bean1.setPlayerName("jojo");
        bean1.setTxnStatus("status");
        initialList.add(bean1);
        final PaymentTransactionData bean2 = new PaymentTransactionData();
        bean2.setCashier("cashier 2");
        bean2.setAmount(300D);
        bean2.setInternalId("internal2");
        bean2.setPlayerId(9833387l);
        bean2.setCurrencyCode("EUR");
        bean2.setDetails("details2");
        bean2.setDate("07/06/2011");
        bean2.setExternalId("external2");
        bean2.setPlayerName("jojo 2");
        bean2.setTxnStatus("status 2");
        initialList.add(bean2);

        given(
                dao.getPaymentTransactionData(eq(new DateTime(2011, 6, 7, 0, 0, 0, 0)), eq(new DateTime(2011,
                        6, 7, 0, 0, 0, 0)), eq("EUR"), eq("Wirecard"), eq("SUCCESS")))
                .willReturn(initialList);

        final Map<String, Double> rates = new LinkedHashMap<String, Double>();
        rates.put(EUR_CODE, 1.5);
        rates.put(USD_CODE, 1.7);
        given(ratesDef.getConversionRates()).willReturn(rates);

        // WHEN the request is received
        final ModelAndView modelAndView = underTest.processCommand(command);
        @SuppressWarnings("unchecked")
        final List<PaymentReportData> actualData =
                (List<PaymentReportData>) modelAndView.getModelMap().get(ReportConstants.REPORT_DATA_MODEL);

        // THEN the appropriate model/view combination is returned
        final List<PaymentTransactionData> expectedData = new ArrayList<PaymentTransactionData>();

        final PaymentTransactionData expectedBean1 = new PaymentTransactionData();
        expectedBean1.setCashier("cashier 1");
        expectedBean1.setAmount(100D);
        expectedBean1.setInternalId("internal");
        expectedBean1.setPlayerId(9887l);
        expectedBean1.setCurrencyCode("USD");
        expectedBean1.setDetails("details");
        expectedBean1.setDate("07/06/2011");
        expectedBean1.setExternalId("external");
        expectedBean1.setPlayerName("jojo");
        expectedBean1.setTxnStatus("status");
        expectedBean1.setGbpAmount(expectedBean1.getAmount() / 1.7);
        expectedData.add(expectedBean1);
        final PaymentTransactionData expectedBean2 = new PaymentTransactionData();
        expectedBean2.setCashier("cashier 2");
        expectedBean2.setAmount(300D);
        expectedBean2.setInternalId("internal2");
        expectedBean2.setPlayerId(9833387l);
        expectedBean2.setCurrencyCode("EUR");
        expectedBean2.setDetails("details2");
        expectedBean2.setDate("07/06/2011");
        expectedBean2.setExternalId("external2");
        expectedBean2.setPlayerName("jojo 2");
        expectedBean2.setTxnStatus("status 2");
        expectedBean2.setGbpAmount(expectedBean2.getAmount() / 1.5);
        expectedData.add(expectedBean2);

        assertEquals("paymentTransactionReportDefinition", modelAndView.getViewName());
        assertEquals(expectedData, actualData);
    }

    @Test
    public void shouldCreateReportForTxnId() {
        // GIVEN the controller receives a request of data per date
        final ReportDefinitionCommand command = new ReportDefinitionCommand();
        command.setTransactionId("internal");
        final Map<String, Double> rates = new LinkedHashMap<String, Double>();
        rates.put(EUR_CODE, 1.5);
        rates.put(USD_CODE, 1.7);
        rates.put(GBP_CODE, 1.0);
        given(ratesDef.getConversionRates()).willReturn(rates);
        command.setPaymentTransactionReportType("txn");

        final List<PaymentTransactionData> initialList = new ArrayList<PaymentTransactionData>();
        final PaymentTransactionData bean1 = new PaymentTransactionData();
        bean1.setCashier("cashier 1");
        bean1.setAmount(100D);
        bean1.setInternalId("internal");
        bean1.setPlayerId(9887l);
        bean1.setCurrencyCode("GBP");
        bean1.setDetails("details");
        bean1.setDate("07/06/2011");
        bean1.setExternalId("external");
        bean1.setPlayerName("jojo");
        bean1.setTxnStatus("status");
        initialList.add(bean1);

        given(dao.getPaymentTransactionData(eq("internal"))).willReturn(initialList);

        // WHEN the request is received
        final ModelAndView modelAndView = underTest.processCommand(command);
        @SuppressWarnings("unchecked")
        final List<PaymentReportData> actualData =
                (List<PaymentReportData>) modelAndView.getModelMap().get(ReportConstants.REPORT_DATA_MODEL);

        // THEN the appropriate model/view combination is returned
        final List<PaymentTransactionData> expectedData = new ArrayList<PaymentTransactionData>();

        final PaymentTransactionData expectedBean1 = new PaymentTransactionData();
        expectedBean1.setCashier("cashier 1");
        expectedBean1.setAmount(100D);
        expectedBean1.setInternalId("internal");
        expectedBean1.setPlayerId(9887l);
        expectedBean1.setCurrencyCode("GBP");
        expectedBean1.setDetails("details");
        expectedBean1.setDate("07/06/2011");
        expectedBean1.setExternalId("external");
        expectedBean1.setPlayerName("jojo");
        expectedBean1.setTxnStatus("status");
        expectedBean1.setGbpAmount(100d);
        expectedData.add(expectedBean1);

        assertEquals("paymentTransactionReportDefinition", modelAndView.getViewName());
        assertEquals(expectedData, actualData);
    }

    @Test
    public void testPaymentTransactionReportDefinition() {
        // GIVEN the conversion rates
        final Map<String, Double> rates = new LinkedHashMap<String, Double>();
        rates.put("USD", 3D);
        rates.put("EUR", 2D);
        given(ratesDef.getConversionRates()).willReturn(rates);

        // WHEN txn report definition is callesd
        final ModelAndView initialData = underTest.showReportForm();

        // THEN default values for form fields should be set
        final ReportDefinitionCommand command =
                (ReportDefinitionCommand) initialData.getModel().get("command");
        assertEquals("html", command.getReportFormat());
        final String yesterday = new DateTime().minusDays(1).toString("yyyy-MM-dd");
        assertThat(command.getFromDate(), is(yesterday));
        assertThat(command.getToDate(), is(yesterday));
        assertThat(command.getPaymentMethod(), is(""));
        assertThat(command.getCurrencyCode(), is(""));
        assertThat(command.getTxnStatus(), is(""));
        assertThat(initialData.getViewName(), is("paymentTransactionReportDefinition"));
    }

    @Test
    public void shouldExposeDataFormatter() {
        // GIVEN the controller instance

        // WHEN asking for the data formatter
        final DataFormatHelper helper = underTest.getFormatter();

        // THEN the unique instance is returned
        assertTrue(helper == DataFormatHelper.getInstance());
    }
}
