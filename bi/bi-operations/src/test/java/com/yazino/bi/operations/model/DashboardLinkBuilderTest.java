package com.yazino.bi.operations.model;

import com.yazino.platform.player.Gender;
import com.yazino.platform.player.PlayerProfileRole;
import com.yazino.platform.player.PlayerProfileStatus;
import com.yazino.platform.player.PlayerSummary;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import static java.math.BigDecimal.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class DashboardLinkBuilderTest {

    private DashboardLinkBuilder underTest;
    private DashboardParameters parameters;

    @Before
    public void setUp() throws Exception {
        parameters = createDashBoardParameters();
        underTest = new DashboardLinkBuilder(aPlayer(), parameters);
    }

    @Test
    public void getPageLinkShouldReturnTheCorrectStringIfSortOrderSpecified() {

        String returnValue = underTest.getPageLink("/bi-operations/player/search", 1);

        String expectedValue = "/bi-operations/player/search?query=12345.0&pageSize=50&dashboardToDisplay=STATEMENT"
                + "&selectionDate=2013-06-12T10%3A30&tableDetail=testTableDetail&fromDate=2013-06-12T10%3A30"
                + "&toDate=2013-06-12T10%3A30&table=testTable&gameType=testGameType&_statementConsolidation=on"
                + "&provider=testProvider&reference=testReference&externalId=45678&search=Search&firstRecord=1"
                + "&pagedRequest=true&sortOrder=testSortOrder";
        assertThat(returnValue, is(expectedValue));
    }

    @Test
    public void getPageLinkShouldReturnTheCorrectStringWithZeroSortOrderIfSortOrderNotSpecified() {

        parameters.setSortOrder(null);
        String returnValue = underTest.getPageLink("/bi-operations/player/search", 1);

        String expectedValue = "/bi-operations/player/search?query=12345.0&pageSize=50&dashboardToDisplay=STATEMENT"
                + "&selectionDate=2013-06-12T10%3A30&tableDetail=testTableDetail&fromDate=2013-06-12T10%3A30"
                + "&toDate=2013-06-12T10%3A30&table=testTable&gameType=testGameType&_statementConsolidation=on"
                + "&provider=testProvider&reference=testReference&externalId=45678&search=Search&firstRecord=1"
                + "&pagedRequest=true";
        assertThat(returnValue, is(expectedValue));
    }

    @Test
    public void getSortLinkShouldReturnCorrectStringIfFirstRecordIsNotSet() {
        String returnValue = underTest.getSortLink("/bi-operations/player/search", "sortString");

        String expectedValue = "/bi-operations/player/search/12345.0/STATEMENT?query=12345.0&pageSize=50"
                + "&dashboardToDisplay=STATEMENT"
                + "&selectionDate=2013-06-12T10%3A30&tableDetail=testTableDetail&fromDate=2013-06-12T10%3A30"
                + "&toDate=2013-06-12T10%3A30&table=testTable&gameType=testGameType&_statementConsolidation=on"
                + "&provider=testProvider&reference=testReference&externalId=45678&sortOrder=sortString"
                + "&firstRecord=0";
        assertThat(returnValue, is(expectedValue));
    }

    private DashboardParameters createDashBoardParameters() {

        DashboardParameters dashboardParameters = new DashboardParameters();
        Date someRandomDate = new DateTime(2013, 6, 12, 10, 30).toDate();
        dashboardParameters.setSelectionDate(someRandomDate);
        dashboardParameters.setTableDetail("testTableDetail");
        dashboardParameters.setFromDate(someRandomDate);
        dashboardParameters.setToDate(someRandomDate);
        dashboardParameters.setTable("testTable");
        dashboardParameters.setGameType("testGameType");
        dashboardParameters.setStatementConsolidation(new HashSet<String>());
        dashboardParameters.setProvider("testProvider");
        dashboardParameters.setReference("testReference");
        dashboardParameters.setPageSize(50);
        dashboardParameters.setSortOrder("testSortOrder");
        return dashboardParameters;
    }

    private PlayerSummary aPlayer() {
        return new PlayerSummary(new BigDecimal("12345.0"), BigDecimal.valueOf(100), "anAvatarUrl",
                new DateTime(3141592L), new DateTime(98765432100L), BigDecimal.valueOf(10), "aRealName",
                "aDisplayName", "anEmailAddress", "aProviderName", "45678", "aCountryCode",
                Gender.OTHER, PlayerProfileStatus.ACTIVE, PlayerProfileRole.CUSTOMER, valueOf(20000),
                Collections.<String, BigDecimal>emptyMap(), Collections.<String, Integer>emptyMap(), Collections.<String>emptySet());
    }
}
