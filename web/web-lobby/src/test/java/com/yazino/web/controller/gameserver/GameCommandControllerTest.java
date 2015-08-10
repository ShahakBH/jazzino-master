package com.yazino.web.controller.gameserver;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.table.Command;
import com.yazino.platform.table.TableService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.math.BigDecimal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GameCommandControllerTest {
    private static final String TEXT_HTML = "text/html";
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private TableService tableService;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private PrintWriter writer;

    private GameCommandController underTest;
    private LobbySession lobbySession;

    @Before
    public void init() throws IOException {
        MockitoAnnotations.initMocks(this);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(12000000L);

        lobbySession = new LobbySession(SESSION_ID, PLAYER_ID, "", "", Partner.YAZINO, null, "", null, false, Platform.WEB, AuthProvider.YAZINO);

        when(response.getWriter()).thenReturn(writer);
        when(request.getCookies()).thenReturn(new Cookie[0]);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);

        underTest = new GameCommandController(tableService, lobbySessionCache);
    }

    @After
    public void resetTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void multipleCommandsAreWrittenToTheTableService() throws Exception {
        setUpReaderWithCommand("11|1|typeA|arg1|arg2\n12|2|typeB|arg3|arg4");
        final Command wrapper1 = new Command(BigDecimal.valueOf(11), 1L, lobbySession.getPlayerId(), SESSION_ID, "typeA", "arg1", "arg2");
        final Command wrapper2 = new Command(BigDecimal.valueOf(12), 2L, lobbySession.getPlayerId(), SESSION_ID, "typeB", "arg3", "arg4");

        underTest.handleCommand(request, response);

        verify(tableService).asyncSendCommand(withCurrentTime(wrapper1));
        verify(tableService).asyncSendCommand(withCurrentTime(wrapper2));
        verify(writer).write("OK");
        verify(response).setContentType(TEXT_HTML);
    }

    @Test
    public void noCommandsAreSentWhenOneIsWrong() throws Exception {
        setUpReaderWithCommand("11|1|typeA|arg1|arg2\n12|2");

        underTest.handleCommand(request, response);

        verify(writer).write(GameCommandController.EPIC_FAIL);
        verifyZeroInteractions(tableService);
    }

    @Test
    public void testCommandCorrectlyConstructed() throws Exception {
        final String postedString = "11|1|typeA|arg1|arg2";

        Command commandWrapper = underTest.constructCommand(BigDecimal.valueOf(12), lobbySession.getSessionId(), postedString);

        assertEquals(BigDecimal.valueOf(12), commandWrapper.getPlayerId());
        assertEquals(BigDecimal.valueOf(11L), commandWrapper.getTableId());
        assertEquals(new Long(1L), commandWrapper.getGameId());
        assertEquals("typeA", commandWrapper.getType());
        assertEquals(2, commandWrapper.getArgs().length);
        assertEquals("arg1", commandWrapper.getArgs()[0]);
        assertEquals("arg2", commandWrapper.getArgs()[1]);
    }

    @Test
    public void testCommandInvalidTable() throws Exception {
        final String postedString = "-1|1|typeA|arg1|arg2";
        assertNull(underTest.constructCommand(BigDecimal.valueOf(111), lobbySession.getSessionId(), postedString));
    }

    @Test
    public void testNumberOfParametersException() {
        String postedString = "12|11";

        try {
            underTest.constructCommand(BigDecimal.valueOf(111), lobbySession.getSessionId(), postedString);
            fail();

        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid number of parameters passed in POST"));
        }
    }

    @Test
    public void testNumberFormatException() {
        String postedString = "12|Invalid|Type Hello";

        try {
            underTest.constructCommand(BigDecimal.valueOf(111), lobbySession.getSessionId(), postedString);
            fail();

        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("For input string: \"Invalid\""));
        }
    }

    @Test
    public void anIOExceptionWhileReadingTheCommandProcessesAsManyCommandsAsItWasAbleToRead() throws Exception {
        final Command command = new Command(BigDecimal.valueOf(11), 1L, lobbySession.getPlayerId(), SESSION_ID, "typeA", "arg1", "arg2");
        final BufferedReader reader = mock(BufferedReader.class);
        when(reader.readLine())
                .thenReturn("11|1|typeA|arg1|arg2")
                .thenThrow(new IOException("aTestIOException"));
        when(request.getReader()).thenReturn(reader);

        underTest.handleCommand(request, response);

        verify(tableService).asyncSendCommand(withCurrentTime(command));
        verify(writer).write("OK");
    }

    private Command withCurrentTime(final Command command) {
        return command.withTimestamp(new DateTime().toDate());
    }

    private void setUpReaderWithCommand(final String commandString) throws IOException {
        final BufferedReader bufferedReader = new BufferedReader(new StringReader(commandString));
        when(request.getReader()).thenReturn(bufferedReader);
    }

}
