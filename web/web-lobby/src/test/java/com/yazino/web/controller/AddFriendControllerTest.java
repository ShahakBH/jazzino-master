package com.yazino.web.controller;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class AddFriendControllerTest {

    private static final BigDecimal FRIEND_ID = BigDecimal.valueOf(100);
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(50);

    @Mock
    private CommunityService communityService;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private final ByteArrayOutputStream responseOut = new ByteArrayOutputStream();
    private final PrintWriter responseOutWriter = new PrintWriter(responseOut);

    private AddFriendController underTest;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        when(lobbySessionCache.getActiveSession(request)).thenReturn(
                new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "aPlayerName", "aSessionKey", Partner.YAZINO, "aPicture", "anEmail", null, false,
                        Platform.WEB, AuthProvider.YAZINO));

        when(response.getWriter()).thenReturn(responseOutWriter);

        underTest = new AddFriendController(communityService, lobbySessionCache);
    }

    @Test(expected = RuntimeException.class)
    public void ifNoSessionIsPresentThenARuntimeExceptionIsThrown() throws IOException {
        reset(lobbySessionCache);

        underTest.addFriend(request, response, FRIEND_ID.toPlainString());
    }

    @Test
    public void anInvalidFriendIdReturnsABadRequestStatus() throws IOException {
        underTest.addFriend(request, response, "anInvalidFriendId");

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void aSuccessfulRequestReturnsSuccessAsTrue() throws IOException {
        underTest.addFriend(request, response, FRIEND_ID.toPlainString());

        responseOutWriter.flush();
        assertThat(responseOut.toString(), is(equalTo("{success:true}")));
    }

    @Test
    public void aSuccessfulRequestCallsTheCommunityService() throws IOException {
        underTest.addFriend(request, response, FRIEND_ID.toPlainString());

        verify(communityService).requestRelationshipChange(PLAYER_ID, FRIEND_ID, RelationshipAction.ADD_FRIEND);
    }

    @Test
    public void anUnsuccessfulRequestReturnsSuccessAsFalse() throws IOException {
        doThrow(new RuntimeException("aTestException")).when(communityService)
                .requestRelationshipChange(PLAYER_ID, FRIEND_ID, RelationshipAction.ADD_FRIEND);

        underTest.addFriend(request, response, FRIEND_ID.toPlainString());

        responseOutWriter.flush();
        assertThat(responseOut.toString(), is(equalTo("{success:false}")));
    }

    @Test
    public void aSuccessfulRequestReturnsJsonContent() throws IOException {
        underTest.addFriend(request, response, FRIEND_ID.toPlainString());

        verify(response).setContentType("application/json");
    }

}
