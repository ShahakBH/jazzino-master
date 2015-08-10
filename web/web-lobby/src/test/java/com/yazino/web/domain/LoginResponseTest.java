package com.yazino.web.domain;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.LoginResult;
import com.yazino.web.session.LobbySession;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class LoginResponseTest {

    @Test(expected = NullPointerException.class)
    public void aLoginResponseCannotBeCreatedWithANullResult() {
        new LoginResponse(null, aLobbySession());
    }

    @Test
    public void aNewUserLoginResponseCanBeCreatedWithTheResultAndTheSession() {
        final LoginResponse response = new LoginResponse(LoginResult.NEW_USER, aLobbySession());

        assertThat(response.getResult(), is(equalTo(LoginResult.NEW_USER)));
        assertThat(response.getSession().get(), is(equalTo(aLobbySession())));
    }

    @Test(expected = NullPointerException.class)
    public void aNewUserLoginResponseCannotBeCreatedWithANullSession() {
        new LoginResponse(LoginResult.NEW_USER, null);
    }

    @Test
    public void anExistingUserLoginResponseCanBeCreatedWithTheResultAndTheSession() {
        final LoginResponse response = new LoginResponse(LoginResult.EXISTING_USER, aLobbySession());

        assertThat(response.getResult(), is(equalTo(LoginResult.EXISTING_USER)));
        assertThat(response.getSession().get(), is(equalTo(aLobbySession())));
    }

    @Test(expected = NullPointerException.class)
    public void anExistingUserLoginResponseCannotBeCreatedWithANullSession() {
        new LoginResponse(LoginResult.EXISTING_USER, null);
    }

    @Test
    public void aFailureLoginResponseCanBeCreatedWithTheResultAndANullSession() {
        final LoginResponse response = new LoginResponse(LoginResult.FAILURE);

        assertThat(response.getResult(), is(equalTo(LoginResult.FAILURE)));
        assertThat(response.getSession().isPresent(), is(false));
    }

    @Test
    public void aBlockedLoginResponseCanBeCreatedWithTheResultAndANullSession() {
        final LoginResponse response = new LoginResponse(LoginResult.BLOCKED);

        assertThat(response.getResult(), is(equalTo(LoginResult.BLOCKED)));
        assertThat(response.getSession().isPresent(), is(false));
    }

    private LobbySession aLobbySession() {
        return new LobbySession(BigDecimal.valueOf(3141592), BigDecimal.TEN, "aPlayerName", "aSessionKey", Partner.YAZINO, "aPictureUrl", "anEmailAddress", null, true, Platform.WEB, AuthProvider.YAZINO);
    }

}
