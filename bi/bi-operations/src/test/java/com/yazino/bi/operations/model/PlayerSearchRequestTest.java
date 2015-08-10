package com.yazino.bi.operations.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: adjanogly
 * Date: 19/01/2011
 * Time: 12:30
 * To change this template use File | Settings | File Templates.
 */
public class PlayerSearchRequestTest {

    @Test
    public void returnStatementAsTheDefault() {
        assertPlayerDashboardValueReturnedCorrect(null, PlayerDashboard.STATEMENT);
    }

    @Test
    public void returnDashboardValueWhenNotNull() {
        assertPlayerDashboardValueReturnedCorrect(PlayerDashboard.GAME, PlayerDashboard.GAME);
    }

    private void assertPlayerDashboardValueReturnedCorrect(PlayerDashboard playerDashboard, PlayerDashboard exepectedResult) {
        DashboardParameters playerSearchRequest = new DashboardParameters();
        playerSearchRequest.setDashboardToDisplay(playerDashboard);

        assertEquals(exepectedResult, playerSearchRequest.getDashboardToDisplay());
    }
}
