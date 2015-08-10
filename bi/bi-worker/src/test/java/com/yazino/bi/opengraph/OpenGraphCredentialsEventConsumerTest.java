package com.yazino.bi.opengraph;

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;

import static org.mockito.Mockito.*;

public class OpenGraphCredentialsEventConsumerTest {

    private static final BigInteger PLAYER_ID = BigInteger.TEN;
    private static final String GAME_TYPE = "slots";
    private static final String ACCESS_TOKEN = "some token";

    private OpenGraphCredentialsEventConsumer underTest;
    private AccessTokenStore accessTokenStore = mock(AccessTokenStore.class);

    @Before
    public void setUp() {
        underTest = new OpenGraphCredentialsEventConsumer(accessTokenStore);
    }

    @Test
    public void shouldDelegateToOpenGraphManager() {
        underTest.handle(new OpenGraphCredentialsMessage(PLAYER_ID, GAME_TYPE, ACCESS_TOKEN));

        verify(accessTokenStore).storeAccessToken(new AccessTokenStore.Key(PLAYER_ID, GAME_TYPE), new AccessTokenStore.AccessToken(ACCESS_TOKEN));
    }

    @Test
    public void shouldNotPropagateException() {
        doThrow(new RuntimeException("test exception")).
                when(accessTokenStore).storeAccessToken(any(AccessTokenStore.Key.class), new AccessTokenStore.AccessToken(any(String.class)));

        underTest.handle(new OpenGraphCredentialsMessage(PLAYER_ID, GAME_TYPE, ACCESS_TOKEN));
    }
}
