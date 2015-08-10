package com.yazino.web.controller;

import com.yazino.web.service.PlayerStatsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerStatsMonitorControllerTest {

    @Mock
    private HttpServletResponse response;
    @Mock
    private PlayerStatsService playerStatsService;

    private ByteArrayOutputStream output = new ByteArrayOutputStream();

    private PlayerStatsMonitorController underTest;
    private PrintWriter responseWriter = new PrintWriter(output);

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        when(response.getWriter()).thenReturn(responseWriter);

        underTest = new PlayerStatsMonitorController(playerStatsService);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullService() {
        new PlayerStatsMonitorController(null);
    }

    @Test
    public void theMonitorReturnsBadRequestIfGivenANullGameType() throws IOException {
        underTest.playerStatsMonitor(null, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void theMonitorReturnsBadRequestIfGivenABlankGameType() throws IOException {
        underTest.playerStatsMonitor("  ", response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void theMonitorSetAContentTypeOfApplicationJSON() throws IOException {
        underTest.playerStatsMonitor("aGameType", response);

        verify(response).setContentType("application/json");
    }

    @Test
    public void theMonitorCallsThePlayerStatsService() throws IOException {
        underTest.playerStatsMonitor("aGameType", response);

        verify(playerStatsService).getAchievementDetails("aGameType");
    }

    @Test
    public void theMonitorStatusIsOkayIfNoExceptionIsThrown() throws IOException {
        underTest.playerStatsMonitor("aGameType", response);

        responseWriter.flush();
        assertThat(output.toString(), is(equalTo("{\"status\":\"okay\"}")));
    }

    @Test
    public void theMonitorStatusIsErrorIfAnExceptionIsThrown() throws IOException {
        when(playerStatsService.getAchievementDetails("aGameType"))
                .thenThrow(new RuntimeException("aTestException"));

        underTest.playerStatsMonitor("aGameType", response);

        responseWriter.flush();
        assertThat(output.toString(), is(equalTo("{\"status\":\"error\"}")));
    }

    @Test
    public void theMonitorReturnsInternalServerErrorIfAnExceptionIsThrown() throws IOException {
        when(playerStatsService.getAchievementDetails("aGameType"))
                .thenThrow(new RuntimeException("aTestException"));

        underTest.playerStatsMonitor("aGameType", response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

}
