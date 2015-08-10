package com.yazino.bi.operations.persistence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static com.yazino.bi.operations.persistence.PostgresPlayerInformationDao.*;

@RunWith(MockitoJUnitRunner.class)
public class PostgresPlayerInformationDaoTest {

    private PostgresPlayerInformationDao underTest = new PostgresPlayerInformationDao(null);

    @Test
    public void sanitizeDashboardFieldsShouldReplaceValueOfFieldsDateStakeReturnWinLossAndApproximateLastBalance() {

        Map<String, String> dashboardFields = createDashBoardFields();
        Map<String, String> sanitizedDashBoard = underTest.sanitizeDashboardFieldsForCorrectSortType(dashboardFields);

        assertThat(sanitizedDashBoard.get(DATE_TIME), is(equalTo("transaction_ts")));
        assertThat(sanitizedDashBoard.get(STAKE), is(equalTo("stake")));
        assertThat(sanitizedDashBoard.get(RETURN), is(equalTo("return")));
        assertThat(sanitizedDashBoard.get(WIN_LOSS), is(equalTo("winloss")));
        assertThat(sanitizedDashBoard.get(APPROX_LAST_BALANCE), is(equalTo("bal")));
    }

    private Map<String, String> createDashBoardFields() {

        final Map<String, String> dashboardFields = new LinkedHashMap<String, String>();
        dashboardFields.put(DATE_TIME, "MAX(tl.TRANSACTION_TS) as transaction_ts");
        dashboardFields.put(TABLE, "tl.table_id");
        dashboardFields.put(GAME, "tl.game_id");
        dashboardFields.put(GAME_TYPE, "ti.GAME_TYPE");
        dashboardFields.put(TABLE_NAME, "ti.TABLE_NAME");
        dashboardFields.put(STAKE, "-SUM(tl.AMOUNT*((tl.TRANSACTION_TYPE='Stake')::integer)) as stake");
        dashboardFields.put(RETURN, "SUM(tl.AMOUNT*((tl.TRANSACTION_TYPE='Return')::integer))as return");
        dashboardFields.put(WIN_LOSS, "SUM(tl.AMOUNT) as winloss");
        dashboardFields.put(SLOTS_BONUS_FIELD, "''");
        dashboardFields.put(APPROX_LAST_BALANCE, "(SELECT a.RUNNING_BALANCE FROM TRANSACTION_LOG a "
                + "WHERE a.ACCOUNT_ID = ? AND a.TRANSACTION_TS=MAX(tl.TRANSACTION_TS) LIMIT 1) as bal");

        return dashboardFields;
    }


}
