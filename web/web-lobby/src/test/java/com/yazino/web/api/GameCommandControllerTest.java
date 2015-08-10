package com.yazino.web.api;

import com.google.common.base.Charsets;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.table.Command;
import com.yazino.platform.table.TableService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GameCommandControllerTest {
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(234L);
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(11L);

    @Mock
    private HttpServletResponse response;
    @Mock
    private TableService tableService;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private PrintWriter writer;

    private MockHttpServletRequest request = new MockHttpServletRequest();

    private GameCommandController underTest;

    @Before
    public void init() throws IOException {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(12000000L);

        when(response.getWriter()).thenReturn(writer);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(aLobbySession());

        underTest = new GameCommandController(tableService, lobbySessionCache);
    }

    @After
    public void resetTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void multipleCommandsAreWrittenToTheTableService() throws Exception {
        setUpReaderWithCommand("11|1|typeA|arg1|arg2\n12|2|typeB|arg3|arg4");
        final Command wrapper1 = new Command(TABLE_ID, 1L, PLAYER_ID, SESSION_ID, "typeA", "arg1", "arg2");
        final Command wrapper2 = new Command(BigDecimal.valueOf(12), 2L, PLAYER_ID, SESSION_ID, "typeB", "arg3", "arg4");

        underTest.postCommand(request, response);

        verify(tableService).asyncSendCommand(withCurrentTime(wrapper1));
        verify(tableService).asyncSendCommand(withCurrentTime(wrapper2));
        verifyJsonResponseContentIs("{}");
    }

    @Test
    public void noCommandsAreSentWhenOneIsWrong() throws Exception {
        setUpReaderWithCommand("11|1|typeA|arg1|arg2\n12|2");

        underTest.postCommand(request, response);

        verifyZeroInteractions(tableService);
    }

    @Test
    public void aBadRequestIsReturnedForAnInvalidCommand() throws Exception {
        setUpReaderWithCommand("11|1|typeA|arg1|arg2\n12|2");

        underTest.postCommand(request, response);

        verifyJsonResponseContentIs("{\"error\":\"invalid_command\"}");
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void aBadRequestIsReturnedForGetRequests() throws Exception {
        underTest.getCommand(response);

        verifyJsonResponseContentIs("{\"error\":\"invalid_method\"}");
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void anUnauthorisedErrorIsReturnedWhenNoSessionIsPresent() throws Exception {
        reset(lobbySessionCache);
        setUpReaderWithCommand("11|1|typeA|arg1|arg2");

        underTest.postCommand(request, response);

        verifyJsonResponseContentIs("{\"error\":\"no_session\"}");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyZeroInteractions(tableService);
    }

    @Test
    public void aServerErrorIsReturnedWhenAnUnexpectedExceptionIsThrown() throws Exception {
        doThrow(new NullPointerException("aTestException")).when(tableService).asyncSendCommand(any(Command.class));
        setUpReaderWithCommand("11|1|typeA|arg1|arg2");

        underTest.postCommand(request, response);

        verifyJsonResponseContentIs("{\"error\":\"server_error\"}");
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test(expected = IllegalArgumentException.class)
    public void anIllegalArgumentExceptionIsThrownIfConstructionOfANullCommandIsRequested() {
        underTest.constructCommand(PLAYER_ID, SESSION_ID, null);
    }

    @Test
    public void aCommandIsCorrectlyConstructed() throws Exception {
        final String postedString = "11|1|typeA|arg1|arg2";

        Command commandWrapper = underTest.constructCommand(PLAYER_ID, SESSION_ID, postedString);

        assertThat(commandWrapper.getPlayerId(), is(equalTo(PLAYER_ID)));
        assertThat(commandWrapper.getTableId(), is(equalTo(TABLE_ID)));
        assertThat(commandWrapper.getGameId(), is(equalTo(1L)));
        assertThat(commandWrapper.getType(), is(equalTo("typeA")));
        assertThat(commandWrapper.getArgs().length, is(equalTo(2)));
        assertThat(commandWrapper.getArgs()[0], is(equalTo("arg1")));
        assertThat(commandWrapper.getArgs()[1], is(equalTo("arg2")));
    }

    @Test
    public void constructingAnInvalidCommandReturnsNull() throws Exception {
        final String postedString = "-1|1|typeA|arg1|arg2";
        assertNull(underTest.constructCommand(BigDecimal.valueOf(111), SESSION_ID, postedString));
    }

    @Test(expected = IllegalArgumentException.class)
    public void anIllegalArgumentExceptionIsThrownWhenConstructingACommandWithAnInvalidNumberOfParameters() {
        underTest.constructCommand(BigDecimal.valueOf(111), SESSION_ID, "12|11");
    }

    @Test(expected = IllegalArgumentException.class)
    public void anIllegalArgumentExceptionIsThrownWhenConstructingACommandWithAnInvalidNumber() {
        underTest.constructCommand(BigDecimal.valueOf(111), SESSION_ID, "12|Invalid|Type Hello");
    }

    @Test(expected = IllegalArgumentException.class)
    public void anIllegalArgumentExceptionIsThrownWhenConstructingACommandWithAnBlankNumber() {
        underTest.constructCommand(BigDecimal.valueOf(111), SESSION_ID, "12||Type Hello");
    }

    @Test
    public void anIOExceptionWhileReadingTheCommandProcessesAsManyCommandsAsItWasAbleToRead() throws Exception {
        final Command command = new Command(TABLE_ID, 1L, PLAYER_ID, SESSION_ID, "typeA", "arg1", "arg2");
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[0]);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(aLobbySession());
        final ServletInputStream reader = new ServletInputStream() {
            private final byte[] content = "11|1|typeA|arg1|arg2\n".getBytes(Charsets.UTF_8);
            private int index;

            @Override
            public int read() throws IOException {
                if (index == content.length) {
                    throw new IOException("aTestIOException");
                }
                return (int) content[index++];
            }

            public boolean isFinished() {
                return false;
            }

            public boolean isReady() {
                return true;
            }

            public void setReadListener(final ReadListener readListener) {

            }
        };
        when(request.getInputStream()).thenReturn(reader);

        underTest.postCommand(request, response);

        verify(tableService).asyncSendCommand(withCurrentTime(command));
        verifyJsonResponseContentIs("{}");
    }

    private Command withCurrentTime(final Command command) {
        return command.withTimestamp(new DateTime().toDate());
    }

    private void setUpReaderWithCommand(final String commandString) throws IOException {
        request.setContent(commandString.getBytes(Charsets.UTF_8));
    }

    private void verifyJsonResponseContentIs(final String responseContent) {
        verify(writer).write(responseContent);
        verify(response).setContentType(JSON_CONTENT_TYPE);
    }

    private LobbySession aLobbySession() {
        return new LobbySession(SESSION_ID, PLAYER_ID, "", "", Partner.YAZINO, null, "", null, false, Platform.WEB, AuthProvider.YAZINO);
    }

}
