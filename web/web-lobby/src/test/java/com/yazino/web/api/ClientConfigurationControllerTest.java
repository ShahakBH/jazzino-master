package com.yazino.web.api;

import com.google.common.collect.ImmutableMap;
import com.yazino.platform.Platform;
import com.yazino.platform.session.SessionClientContextKey;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.service.ClientPropertyService;
import com.yazino.web.service.RememberMeHandler;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.util.WebApiResponseWriter;
import com.yazino.web.util.WebApiResponses;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.util.ObjectUtils.nullSafeEquals;

public class ClientConfigurationControllerTest {

    private static final String PLATFORM = "WEB";
    private static final String GAME_TYPE = "SLOTS";
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(37);

    private ClientPropertyService clientPropertyService = mock(ClientPropertyService.class);
    private LobbySessionCache lobbySessionCache = mock(LobbySessionCache.class);
    private LobbySession lobbySession = mock(LobbySession.class);
    private RememberMeHandler rememberMeHandler = mock(RememberMeHandler.class);
    private MockHttpServletRequest request = new MockHttpServletRequest();
    private MockHttpServletResponse response = new MockHttpServletResponse();
    private WebApiResponses webApiResponses = new WebApiResponses(new WebApiResponseWriter());
    private CookieHelper cookieHelper = mock(CookieHelper.class);
    private static final String CLIENT_CONTEXT = format(
            "{\"%s\":\"unique identifier for device\"}", SessionClientContextKey.DEVICE_ID.name());


    private GameTypeResolver gameTypeResolver = mock(GameTypeResolver.class);

    private ClientConfigurationController underTest = new ClientConfigurationController(
            clientPropertyService, lobbySessionCache, rememberMeHandler, webApiResponses, cookieHelper,
            gameTypeResolver);

    @Test
    public void getConfiguration_v1_0_shouldAddBaseProperties() throws IOException {
        Map<String, Object> expectedProperties =
                ImmutableMap.<String, Object>builder().
                        put("p1", "v1").
                        put("p2", "v2").
                        build();
        when(clientPropertyService.getBasePropertiesFor(Platform.valueOf(PLATFORM))).thenReturn(expectedProperties);

        underTest.getConfiguration_v1_0(request, response, PLATFORM, GAME_TYPE);

        assertThat(response, containsProperties(expectedProperties));
    }

    @Test
    public void getConfiguration_v1_0_shouldAddSessionPropertiesWhenSessionAvailable() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);
        Map<String, Object> expectedProperties =
                ImmutableMap.<String, Object>builder().
                        put("p1", "v1").
                        put("p2", "v2").
                        build();
        when(clientPropertyService.getSessionPropertiesFor(lobbySession)).thenReturn(expectedProperties);

        underTest.getConfiguration_v1_0(request, response, PLATFORM, GAME_TYPE);

        assertThat(response, containsProperties(expectedProperties));
    }

    @Test
    public void getConfiguration_v1_1_shouldAddBaseProperties() throws IOException {
        Map<String, Object> expectedProperties =
                ImmutableMap.<String, Object>builder().
                        put("p1", "v1").
                        put("p2", "v2").
                        build();
        when(clientPropertyService.getBasePropertiesFor(Platform.valueOf(PLATFORM))).thenReturn(expectedProperties);

        underTest.getConfiguration_v1_1(request, response, PLATFORM, GAME_TYPE, CLIENT_ID, false, "{}");

        assertThat(response, containsProperties(expectedProperties));
    }

    @Test
    public void getConfiguration_v1_1_shouldReturnABadRequestIfTheServiceThrowsAnIllegalStateException() throws IOException {
        when(clientPropertyService.getBasePropertiesFor(Platform.valueOf(PLATFORM))).thenThrow(
                new IllegalStateException("aTestException"));

        underTest.getConfiguration_v1_1(request, response, PLATFORM, GAME_TYPE, CLIENT_ID, false, "{}");

        assertThat(response.getStatus(), is(equalTo(HttpServletResponse.SC_BAD_REQUEST)));
        assertThat(response.getContentAsString(), containsString("aTestException"));
    }

    @Test
    public void getConfiguration_v1_1_shouldAddSessionPropertiesWhenSessionAvailable() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);
        Map<String, Object> expectedProperties =
                ImmutableMap.<String, Object>builder().
                        put("p1", "v1").
                        put("p2", "v2").
                        build();
        when(clientPropertyService.getSessionPropertiesFor(lobbySession)).thenReturn(expectedProperties);

        underTest.getConfiguration_v1_1(request, response, PLATFORM, GAME_TYPE, CLIENT_ID, false, "{}");

        assertThat(response, containsProperties(expectedProperties));
    }

    @Test
    public void getConfiguration_v1_1_shouldAddMinimumVersion() throws IOException {
        Map<String, Object> expectedProperties =
                ImmutableMap.<String, Object>builder().
                        put("minimum-version", "1").
                        build();
        when(clientPropertyService.getVersionsFor(Platform.valueOf(PLATFORM), GAME_TYPE, CLIENT_ID)).thenReturn(
                expectedProperties);

        underTest.getConfiguration_v1_1(request, response, PLATFORM, GAME_TYPE, CLIENT_ID, false, "{}");

        assertThat(response, containsProperties(expectedProperties));
    }

    @Test
    public void getConfiguration_v1_1_shouldAddGameAvailability() throws IOException {
        Map<String, Object> expectedProperties =
                ImmutableMap.<String, Object>builder().
                        put("availability", "MAINTENANCE_SCHEDULED").
                        put("countdown", "1").
                        build();
        when(clientPropertyService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(expectedProperties);

        underTest.getConfiguration_v1_1(request, response, PLATFORM, GAME_TYPE, CLIENT_ID, false, "{}");

        assertThat(response, containsProperties(expectedProperties));
    }

    @Test
    public void getConfiguration_v1_1_shouldAddLatestVersion() throws IOException {
        Map<String, Object> expectedProperties =
                ImmutableMap.<String, Object>builder().
                        put("latest-version", "1.1").
                        build();
        when(clientPropertyService.getVersionsFor(Platform.valueOf(PLATFORM), GAME_TYPE, CLIENT_ID)).thenReturn(
                expectedProperties);

        underTest.getConfiguration_v1_1(request, response, PLATFORM, GAME_TYPE, CLIENT_ID, false, "{}");

        assertThat(response, containsProperties(expectedProperties));
    }

    @Test
    public void getConfiguration_v1_1_shouldSetFacebookCanvasFlag() throws IOException {
        Map<String, Object> expectedProperties =
                ImmutableMap.<String, Object>builder().
                        put("facebook-canvas", "true").
                        build();
        when(cookieHelper.isOnCanvas(request)).thenReturn(true);

        underTest.getConfiguration_v1_1(request, response, PLATFORM, GAME_TYPE, CLIENT_ID, false, "{}");

        assertThat(response, containsProperties(expectedProperties));
    }

    @Test
    public void getSessionConfiguration_shouldWriteSessionPropertiesToResponse() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);
        Map<String, Object> expectedProperties =
                ImmutableMap.<String, Object>builder().
                        put("p1", "v1").
                        put("p2", "v2").
                        build();
        when(clientPropertyService.getSessionPropertiesFor(lobbySession)).thenReturn(expectedProperties);

        underTest.getSessionConfiguration(request, response);

        assertThat(response, containsProperties(expectedProperties));
    }

    @Test
    public void getSessionConfiguration_shouldReturnUnauthorisedWhenNoSessionIsPresent() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(null);

        underTest.getSessionConfiguration(request, response);

        assertThat(response.getStatus(), is(equalTo(HttpServletResponse.SC_UNAUTHORIZED)));
    }

    @Test
    public void getSessionConfiguration_shouldReturnAnEmptyMapWhenNoSessionIsPresent() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(null);

        underTest.getSessionConfiguration(request, response);

        assertThat(response, containsProperties(Collections.<String, Object>emptyMap()));
    }

    @Test
    public void clientContextParameterShouldBePassedToTheRememberMeHandlerIfUsingRememberMe() throws IOException {

        underTest.getConfiguration_v1_1(request, response, PLATFORM, GAME_TYPE, CLIENT_ID, true, CLIENT_CONTEXT);
        Map<String, Object> expectedClientContextMap = newHashMap();
        expectedClientContextMap.put("DEVICE_ID", "unique identifier for device");
        verify(rememberMeHandler).attemptAutoLogin(request, response, expectedClientContextMap, GAME_TYPE);
    }

    private Matcher<MockHttpServletResponse> containsProperties(Map<String, Object> expectedProperties) {
        return new ContainsPropertiesMatcher(expectedProperties);
    }

    private class ContainsPropertiesMatcher extends TypeSafeDiagnosingMatcher<MockHttpServletResponse> {
        private final Map<String, Object> expectedProperties;

        public ContainsPropertiesMatcher(Map<String, Object> expectedProperties) {
            this.expectedProperties = expectedProperties;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("map containing all entries in ").appendValue(expectedProperties);
        }

        @Override
        protected boolean matchesSafely(MockHttpServletResponse candidate, Description mismatchDescription) {
            try {
                Map<?, ?> actualProperties = new JsonHelper().deserialize(Map.class, candidate.getContentAsString());
                for (Map.Entry<?, ?> entry : expectedProperties.entrySet()) {
                    Object key = entry.getKey();
                    Object actualValue = actualProperties.get(key);
                    Object expectedValue = entry.getValue();
                    if (!nullSafeEquals(actualValue, expectedValue)) {
                        if (actualValue == null) {
                            mismatchDescription.appendText("no value for key ").appendValue(key);
                        } else {
                            mismatchDescription.appendValue(key).appendText("=").appendValue(actualValue);
                        }
                        return false;
                    }
                }
                return true;
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
