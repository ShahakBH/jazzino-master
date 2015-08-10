package com.yazino.web.controller.gameserver;


import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.web.domain.LaunchConfiguration;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.util.MessagingHostResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LaunchConfigurationControllerTest {

    private static final String HOST = "somewhere-virtual-maggie.london.yazino.com";
    private static final String VIRTUAL_HOST = "somewhere-maggie.london.yazino.com";
    private static final String PORT = "5672";
    private static final String CONTENT_URL = "http://somewhere.london.yazino.com:8188/web-content/";
    private static final String CLIENT_URL = "http://stockwell.london.yazino.com:8188/client-content/";
    private static final String COMMAND_URL = "http://somewhere.london.yazino.com:80/game-server/command/";
    private static final String PERMANENT_CONTENT_URL = "http://cdn.yazino.com/permanent-content-v2/";
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;

    @Mock
    private HttpServletRequest request;

    @Mock
    private MessagingHostResolver messagingHostResolver;

    @Mock
    private LobbySessionCache lobbySessionCache;

    private MockHttpServletResponse response;

    private LaunchConfigurationController underTest;

    @Before
    public void init() throws UnsupportedEncodingException {
        response = new MockHttpServletResponse();
        LaunchConfiguration launchConfiguration = new LaunchConfiguration(HOST, VIRTUAL_HOST, PORT, CONTENT_URL,
                CLIENT_URL, COMMAND_URL, null, PERMANENT_CONTENT_URL);
        underTest = new LaunchConfigurationController(launchConfiguration, messagingHostResolver, lobbySessionCache);
    }

    @Test
    public void contentTypeShouldBeJson() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(aPlayerSession());

        underTest.launchConfiguration(request, response);

        assertThat(response.getContentType(), is("application/json"));
    }

    @Test
    public void characterEncodingShouldBeUtf8() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(aPlayerSession());

        underTest.launchConfiguration(request, response);

        assertThat(response.getCharacterEncoding(), is("UTF-8"));
    }

    @Test
    public void aValidSessionMustBePresentForTheCurrentPlayer() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);

        underTest.launchConfiguration(request, response);

        assertThat(response.getErrorMessage(), is("Your session expired.  Please log in again."));
        assertThat(response.getStatus(), is(401));
    }

    @Test
    public void responseShouldHaveLaunchConfiguration() throws Exception {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(aPlayerSession());
        when(messagingHostResolver.resolveMessagingHostForPlayer(PLAYER_ID)).thenReturn(HOST);

        underTest.launchConfiguration(request, response);

        assertThat(response.getContentAsString(), is(equalTo(expectedConfiguration())));
    }

    private String expectedConfiguration() {
        Map<String, String> configuration = new HashMap<>();
        configuration.put("amqpHost", HOST);
        configuration.put("amqpVirtualHost", VIRTUAL_HOST);
        configuration.put("amqpPort", PORT);
        configuration.put("commandUrl", COMMAND_URL);
        configuration.put("contentUrl", CONTENT_URL);
        configuration.put("clientUrl", CLIENT_URL);
        configuration.put("permanentContentUrl", PERMANENT_CONTENT_URL);
        return new JsonHelper().serialize(configuration);
    }

    private LobbySession aPlayerSession() {
        return new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "aPlayerName", "aSessionKey", Partner.YAZINO,
                "aPictureUrl", "anEmailAddress", null, false, Platform.ANDROID, AuthProvider.YAZINO);
    }
}
