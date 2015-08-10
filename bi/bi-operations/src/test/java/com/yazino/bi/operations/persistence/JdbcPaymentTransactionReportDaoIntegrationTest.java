package com.yazino.bi.operations.persistence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.yazino.bi.operations.view.reportbeans.PaymentTransactionData;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static utils.PlayerBuilder.*;


@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)

public class JdbcPaymentTransactionReportDaoIntegrationTest {

    @Autowired
    JdbcPaymentTransactionReportDao underTest;

    @Autowired
    private NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;

    @Test
    public void getTransactionShouldOnlyReturnSingleTransaction() {
        initialise(externalDwNamedJdbcTemplate);
        createPlayer(ANDY).whoRegistered(today).whoBoughtChips(new Purchase(today, 123)).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(BOB).whoRegistered(today).whoBoughtChips(new Purchase(today, 123)).storeIn(externalDwNamedJdbcTemplate);
        createPlayer(CHAZ).whoRegistered(today).whoBoughtChips(new Purchase(today, 123)).storeIn(externalDwNamedJdbcTemplate);

        final Map<String, Object> params = newHashMap();
        params.put("player_id", ANDY);
        final int internalId = externalDwNamedJdbcTemplate.queryForInt(
                "select internal_transaction_id from external_transaction where player_id=:player_id", params);
        final List<PaymentTransactionData> paymentTransactionData = underTest.getPaymentTransactionData("" + internalId);

        assertThat(paymentTransactionData.size(), is(1));
    }

    @Test
    public void getTransactionsShouldShowFirstPurchaseIfExists() {
        initialise(externalDwNamedJdbcTemplate);
        createPlayer(ANDY).whoRegistered(lastYear).whoBoughtChips(new Purchase(today.minus(10), 123), new Purchase(lastMonth, 666)).storeIn(
                externalDwNamedJdbcTemplate);
        createPlayer(BOB).whoRegistered(today).whoBoughtChips(new Purchase(today, 123).withStatus("started")).storeIn(
                externalDwNamedJdbcTemplate);
        final Map<String, Object> params = newHashMap();
        externalDwNamedJdbcTemplate.update("refresh materialized view EXTERNAL_TRANSACTION_mv", params);

        final List<PaymentTransactionData> paymentTransactionData = underTest.getPaymentTransactionData(yesterday, today,
                null, null, null);

        assertThat(paymentTransactionData.size(), is(2));

        assertThat(paymentTransactionData.get(1).getFirstPurchaseDate().split(" ")[0], is(lastMonth.toString().split("T")[0]));
        assertThat(paymentTransactionData.get(1).getTxnStatus(), is(equalTo("SUCCESS")));

        assertThat(paymentTransactionData.get(0).getFirstPurchaseDate(), nullValue());
        assertThat(paymentTransactionData.get(0).getTxnStatus(), is(equalTo("started")));
    }

}
