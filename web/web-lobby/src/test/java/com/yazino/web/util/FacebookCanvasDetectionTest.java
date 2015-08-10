package com.yazino.web.util;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Platform;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

import static com.yazino.platform.Partner.YAZINO;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class FacebookCanvasDetectionTest {

    private static final LobbySession LOBBY_SESSION = new LobbySession(BigDecimal.valueOf(3141592), new BigDecimal(1), "", "", YAZINO, "", "", null, false,
            Platform.WEB, AuthProvider.YAZINO);
    private static final boolean CANVAS_IS_SET = true, HAS_SESSION = true, OVERRIDE_ENABLED = true;
    private static final boolean CANVAS_IS_NOT_SET = false, HAS_NO_SESSION = false, OVERRIDE_DISABLED = false;

    @Mock
    private FacebookConfiguration facebookConfiguration;
    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private GameTypeResolver gameTypeResolver;
    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private FacebookCanvasDetection underTest;

    @Before
    public void setUp() {
        underTest = new FacebookCanvasDetection(facebookConfiguration, cookieHelper, gameTypeResolver, yazinoConfiguration, lobbySessionCache);
    }

    @Test
    public void shouldResolveIsCanvas() {
        assertThat(resultWhen(CANVAS_IS_SET, OVERRIDE_ENABLED, HAS_SESSION), is(true));
        assertThat(resultWhen(CANVAS_IS_SET, OVERRIDE_ENABLED, HAS_NO_SESSION), is(false));
        assertThat(resultWhen(CANVAS_IS_SET, OVERRIDE_DISABLED, HAS_SESSION), is(true));
        assertThat(resultWhen(CANVAS_IS_SET, OVERRIDE_DISABLED, HAS_NO_SESSION), is(true));
        assertThat(resultWhen(CANVAS_IS_NOT_SET, OVERRIDE_ENABLED, HAS_SESSION), is(false));
        assertThat(resultWhen(CANVAS_IS_NOT_SET, OVERRIDE_ENABLED, HAS_NO_SESSION), is(false));
        assertThat(resultWhen(CANVAS_IS_NOT_SET, OVERRIDE_DISABLED, HAS_SESSION), is(false));
        assertThat(resultWhen(CANVAS_IS_NOT_SET, OVERRIDE_DISABLED, HAS_NO_SESSION), is(false));
    }

    private boolean resultWhen(boolean canvasParameter, boolean overrideEnabled, boolean hasSession) {
        Mockito.reset(cookieHelper, lobbySessionCache, yazinoConfiguration);
        Mockito.when(cookieHelper.isOnCanvas(request)).thenReturn(canvasParameter);
        Mockito.when(lobbySessionCache.getActiveSession(request)).thenReturn(hasSession ? LOBBY_SESSION : null);
        Mockito.when(yazinoConfiguration.getBoolean("facebook.canvas-detection.requires-session", true)).thenReturn(overrideEnabled);
        return underTest.isOnCanvas(request);
    }

}
