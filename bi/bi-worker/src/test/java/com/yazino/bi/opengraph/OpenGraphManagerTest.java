package com.yazino.bi.opengraph;

import com.yazino.platform.opengraph.OpenGraphAction;
import com.yazino.platform.opengraph.OpenGraphObject;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import java.io.IOException;
import java.math.BigInteger;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class OpenGraphManagerTest {

    public final static OpenGraphAction ACTION_1 = new OpenGraphAction("spin", new OpenGraphObject("wheel", "http://sample.url1"));
    public final static OpenGraphAction ACTION_2 = new OpenGraphAction("won", new OpenGraphObject("game", "http://sample.url2"));
    public static final String GAME_TYPE_1 = "slots";
    public static final String GAME_TYPE_2 = "highstakes";
    public static final AccessTokenStore.AccessToken ACCESS_TOKEN_1 = new AccessTokenStore.AccessToken("A");
    public static final AccessTokenStore.AccessToken ACCESS_TOKEN_2 = new AccessTokenStore.AccessToken("B");
    public static final String APP_1_NAMESPACE = "testwheeldeal";
    public static final String APP_2_NAMESPACE = "testhighstakes";
    private static final BigInteger PLAYER_1_ID = BigInteger.ONE;
    private static final BigInteger PLAYER_2_ID = BigInteger.TEN;

    private OpenGraphManager underTest;
    private OpenGraphHttpInvoker openGraphHttpInvoker;
    private AccessTokenStore accessTokenStore;
    private FacebookConfiguration facebookConfiguration = mock(FacebookConfiguration.class);

    @Before
    public void setUp() throws Exception {
        openGraphHttpInvoker = mock(OpenGraphHttpInvoker.class);
        accessTokenStore = mock(AccessTokenStore.class);
        underTest = new OpenGraphManager(facebookConfiguration, accessTokenStore, openGraphHttpInvoker);

        // common mocked calls
        when(openGraphHttpInvoker.hasPermission(ACCESS_TOKEN_1.getAccessToken())).thenReturn(true);
        when(openGraphHttpInvoker.hasPermission(ACCESS_TOKEN_2.getAccessToken())).thenReturn(true);

        final FacebookAppConfiguration fac1 = mock(FacebookAppConfiguration.class);
        final FacebookAppConfiguration fac2 = mock(FacebookAppConfiguration.class);
        when(fac1.getAppName()).thenReturn(APP_1_NAMESPACE);
        when(fac2.getAppName()).thenReturn(APP_2_NAMESPACE);
        when(facebookConfiguration.getAppConfigFor(GAME_TYPE_1,
                FacebookConfiguration.ApplicationType.CANVAS,
                FacebookConfiguration.MatchType.STRICT)).thenReturn(fac1);
        when(facebookConfiguration.getAppConfigFor(GAME_TYPE_2,
                FacebookConfiguration.ApplicationType.CANVAS,
                FacebookConfiguration.MatchType.STRICT)).thenReturn(fac2);

    }


    @Test
    public void shouldPublishActionWithAccessTokenForCorrectUser() throws IOException {
        when(accessTokenStore.findByKey(new AccessTokenStore.Key(PLAYER_1_ID, GAME_TYPE_1))).thenReturn(ACCESS_TOKEN_1);
        when(accessTokenStore.findByKey(new AccessTokenStore.Key(PLAYER_2_ID, GAME_TYPE_1))).thenReturn(ACCESS_TOKEN_2);

        underTest.publishAction(ACTION_1, PLAYER_1_ID, GAME_TYPE_1);
        verify(openGraphHttpInvoker).publishAction(argThat(Matchers.is(ACCESS_TOKEN_1.getAccessToken())), argThat(is(any(OpenGraphAction.class))), argThat(is(any(String.class))));

        underTest.publishAction(ACTION_1, PLAYER_2_ID, GAME_TYPE_1);
        verify(openGraphHttpInvoker).publishAction(argThat(Matchers.is(ACCESS_TOKEN_2.getAccessToken())), argThat(is(any(OpenGraphAction.class))), argThat(is(any(String.class))));
    }

    @Test
    public void shouldPublishActionWithAccessTokenForCorrectGameType() throws IOException {
        when(accessTokenStore.findByKey(new AccessTokenStore.Key(PLAYER_1_ID, GAME_TYPE_1))).thenReturn(ACCESS_TOKEN_1);
        when(accessTokenStore.findByKey(new AccessTokenStore.Key(PLAYER_1_ID, GAME_TYPE_2))).thenReturn(ACCESS_TOKEN_2);

        underTest.publishAction(ACTION_1, PLAYER_1_ID, GAME_TYPE_1);
        verify(openGraphHttpInvoker).publishAction(argThat(Matchers.is(ACCESS_TOKEN_1.getAccessToken())), argThat(is(any(OpenGraphAction.class))), argThat(is(any(String.class))));

        underTest.publishAction(ACTION_1, PLAYER_1_ID, GAME_TYPE_2);
        verify(openGraphHttpInvoker).publishAction(argThat(Matchers.is(ACCESS_TOKEN_2.getAccessToken())), argThat(is(any(OpenGraphAction.class))), argThat(is(any(String.class))));
    }

    @Test
    public void ignoresEventsWhenNoAccessTokenAvailable() throws IOException {
        when(accessTokenStore.findByKey(new AccessTokenStore.Key(PLAYER_1_ID, "slots"))).thenReturn(null);

        underTest.publishAction(ACTION_1, PLAYER_1_ID, GAME_TYPE_1);

        verify(openGraphHttpInvoker, never()).publishAction(null, null, null);
    }

    @Test
    public void ignoresEventsWithNoActions() throws IOException {
        when(accessTokenStore.findByKey(new AccessTokenStore.Key(PLAYER_1_ID, "slots"))).thenReturn(ACCESS_TOKEN_1);

        underTest.publishAction(ACTION_2, PLAYER_1_ID, GAME_TYPE_1);

        verify(openGraphHttpInvoker, never()).publishAction(null, null, null);
    }


    // should check permission and only send if they are there, should only check once
    @Test
    public void shouldNotPublishIfUserHasNotAuthorisedPublishActions() throws IOException {
        when(accessTokenStore.findByKey(new AccessTokenStore.Key(PLAYER_1_ID, "slots"))).thenReturn(ACCESS_TOKEN_1);
        when(openGraphHttpInvoker.hasPermission(ACCESS_TOKEN_1.getAccessToken())).thenReturn(false);

        underTest.publishAction(ACTION_1, PLAYER_1_ID, GAME_TYPE_1);

        verify(openGraphHttpInvoker, never()).publishAction(null, null, null);
    }

    @Test
    public void shouldInvalidateTokenWhenTokenRejected() throws IOException {
        when(accessTokenStore.findByKey(new AccessTokenStore.Key(PLAYER_1_ID, GAME_TYPE_1))).thenReturn(ACCESS_TOKEN_1);
        when(openGraphHttpInvoker.hasPermission(ACCESS_TOKEN_1.getAccessToken())).thenReturn(true);

        Mockito.doThrow(new InvalidAccessTokenException("some error message")).
                when(openGraphHttpInvoker).publishAction(argThat(Matchers.is(ACCESS_TOKEN_1.getAccessToken())),
                argThat(is(any(OpenGraphAction.class))), argThat(is(any(String.class))));

        underTest.publishAction(ACTION_1, PLAYER_1_ID, GAME_TYPE_1);

        verify(accessTokenStore).invalidateToken(new AccessTokenStore.Key(PLAYER_1_ID, GAME_TYPE_1));
    }

    @Test
    public void shouldNotInvalidateTokenWhenAccepted() throws IOException {
        when(accessTokenStore.findByKey(new AccessTokenStore.Key(PLAYER_1_ID, GAME_TYPE_1))).thenReturn(ACCESS_TOKEN_1);
        when(openGraphHttpInvoker.hasPermission(ACCESS_TOKEN_1.getAccessToken())).thenReturn(true);

        underTest.publishAction(ACTION_1, PLAYER_1_ID, GAME_TYPE_1);

        verify(accessTokenStore, never()).invalidateToken(new AccessTokenStore.Key(PLAYER_1_ID, GAME_TYPE_1));
    }


}
