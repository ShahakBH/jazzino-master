package com.yazino.bi.payment.worldpay;

import com.yazino.bi.payment.persistence.JDBCPaymentChargebackDAO;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.emis.*;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.audit.persistence.PostgresExternalTransactionDAO;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Currency;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WorldPayChargebackUpdaterTest {

    @Mock
    private WorldPayFileServer fileServer;
    @Mock
    private WorldPayChargebacksParser parser;
    @Mock
    private JDBCPaymentChargebackDAO chargebackDao;
    @Mock
    private PostgresExternalTransactionDAO externalTransactionDao;
    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private PlayerChargebackHandler chargebackHandler;

    private WorldPayChargebackUpdater underTest;

    @Before
    public void setUp() throws IOException {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2013, 6, 17, 0, 0, 0, 0).getMillis());

        when(yazinoConfiguration.getBoolean("payment.worldpay.chargeback.update.active", true)).thenReturn(true);
        when(yazinoConfiguration.getString("payment.worldpay.chargeback.update.filename", "'YAZOC'yyyyMMdd'.CSV'")).thenReturn("'YAZOC'yyyyMMdd'.CSV'");
        when(parser.parse(any(InputStream.class))).thenReturn(worldPayChargebacks());
        when(externalTransactionDao.findPlayerIdFor("transaction1")).thenReturn(BigDecimal.valueOf(1));
        when(externalTransactionDao.findPlayerIdFor("transaction2")).thenReturn(BigDecimal.valueOf(2));
        when(externalTransactionDao.findPlayerIdFor("transaction3")).thenReturn(BigDecimal.valueOf(3));

        underTest = new WorldPayChargebackUpdater(fileServer, parser, chargebackDao, externalTransactionDao, chargebackHandler, yazinoConfiguration);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void whenUpdatesAreInactiveThenNoActionsTakePlace() {
        reset(yazinoConfiguration);

        underTest.updateChargebacksForYesterday();

        verifyZeroInteractions(fileServer, parser, chargebackDao);
    }

    @Test
    public void fetchedFilesAreWrittenToATemporaryFile() throws IOException {
        when(fileServer.fetchTo(anyString(), anyString())).thenReturn(true);

        underTest.updateChargebacksForYesterday();

        final ArgumentCaptor<String> destFile = ArgumentCaptor.forClass(String.class);
        verify(fileServer).fetchTo(anyString(), destFile.capture());
        File tmpFile = File.createTempFile("wpfut", ".tmp");
        assertThat(destFile.getValue(), containsString(tmpFile.getParent()));
    }

    @Test
    public void fetchingChargebacksForYesterdayFetchesYesterdaysFile() throws IOException {
        when(fileServer.fetchTo(anyString(), anyString())).thenReturn(true);

        underTest.updateChargebacksForYesterday();

        final ArgumentCaptor<String> sourceFile = ArgumentCaptor.forClass(String.class);
        verify(fileServer).fetchTo(sourceFile.capture(), anyString());
        assertThat(sourceFile.getValue(), endsWith("YAZOC20130616.CSV"));
    }

    @Test
    public void fetchingChargebacksForAnArbitaryDateFetchesTheDatedFile() throws IOException {
        when(fileServer.fetchTo(anyString(), anyString())).thenReturn(true);

        underTest.updateChargebacksFor(new DateTime(2013, 3, 5, 0, 0, 0));

        final ArgumentCaptor<String> sourceFile = ArgumentCaptor.forClass(String.class);
        verify(fileServer).fetchTo(sourceFile.capture(), anyString());
        assertThat(sourceFile.getValue(), endsWith("YAZOC20130305.CSV"));
    }

    @Test
    public void anExceptionWhenFetchingFilesIsNotPropagated() throws WorldPayFileServerException {
        when(fileServer.fetchTo(anyString(), anyString())).thenThrow(new RuntimeException("aTestException"));

        underTest.updateChargebacksForYesterday();

        verifyZeroInteractions(parser, chargebackDao);
    }

    @Test
    public void aFileContainingNoChargebacksIsIgnored() throws IOException {
        when(parser.parse(any(InputStream.class))).thenReturn(
                new WorldPayChargebacks(Collections.<Chargeback>emptyList(), new DateTime(2013, 6, 16, 0, 0, 0)));

        underTest.updateChargebacksForYesterday();

        verifyZeroInteractions(chargebackDao);
    }

    @Test
    public void chargebacksAreWrittenToTheDAO() throws IOException {
        underTest.updateChargebacksForYesterday();

        verify(chargebackDao).save(chargeback(1));
        verify(chargebackDao).save(chargeback(2));
        verify(chargebackDao).save(chargeback(3));
    }

    @Test
    public void chargebacksArePassedToTheHandler() throws IOException {
        underTest.updateChargebacksForYesterday();

        verify(chargebackHandler).handleChargeback(chargeback(1));
        verify(chargebackHandler).handleChargeback(chargeback(2));
        verify(chargebackHandler).handleChargeback(chargeback(3));
    }

    @Test
    public void anExceptionFromTheChargebackHandlerDoesNotStopProcessingOfOtherChargebacks() throws IOException {
        doThrow(new RuntimeException("aTestException")).when(chargebackHandler).handleChargeback(chargeback(1));

        underTest.updateChargebacksForYesterday();

        verify(chargebackHandler).handleChargeback(chargeback(2));
        verify(chargebackHandler).handleChargeback(chargeback(3));
    }

    @Test
    public void chargebacksWithTransactionIDsThatCannotBeMatchedToAPlayerAreIgnored() throws IOException {
        reset(externalTransactionDao);
        when(externalTransactionDao.findPlayerIdFor("transaction1")).thenReturn(BigDecimal.valueOf(1));
        when(externalTransactionDao.findPlayerIdFor("transaction3")).thenReturn(BigDecimal.valueOf(3));

        underTest.updateChargebacksForYesterday();

        verify(chargebackDao).save(chargeback(1));
        verify(chargebackDao, times(0)).save(chargeback(2));
        verify(chargebackDao).save(chargeback(3));
    }

    private WorldPayChargebacks worldPayChargebacks() {
        return new WorldPayChargebacks(
                asList(worldPayChargeback(1), worldPayChargeback(2), worldPayChargeback(3)),
                new DateTime(2013, 6, 16, 0, 0, 0, 0));
    }

    private Chargeback worldPayChargeback(final int id) {
        return new Chargeback(new BigDecimal("10000" + id), "cardNumber" + id, CardScheme.MASTERCARD, "merchant" + id, "transaction" + id,
                new DateTime(2013, 3, id, 0, 0, 0), new BigDecimal(id + "00.00"), Currency.getInstance("GBP"), "ref" + id, "reason" + id,
                new DateTime(2013, 5, id, 0, 0, 0), ChargebackTransactionType.SALE, new BigDecimal(id + "00." + id), "rc" + id);
    }

    private com.yazino.bi.payment.Chargeback chargeback(final int id) {
        return new com.yazino.bi.payment.Chargeback("ref" + id, new DateTime(2013, 5, id, 0, 0, 0), "transaction" + id,
                new DateTime(2013, 3, id, 0, 0, 0), BigDecimal.valueOf(id), null, "rc" + id, "reason" + id, "cardNumber" + id,
                new BigDecimal(id + "00." + id), Currency.getInstance("GBP"));
    }

}
