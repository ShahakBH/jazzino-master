package com.yazino.mobile.ws.ios;

import com.yazino.mobile.ws.config.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.mobile.ws.ModelAttributeKeys.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IOSControllerTest {
    @Mock
    private LightstreamerConfig lightstreamerConfig;
    @Mock
    private IOSConfig iosConfig;
    @Mock
    private FacebookConfig facebookConfig;
    @Mock
    private GamesConfig gamesConfig;
    @Mock
    private TapjoyConfig tapjoyConfig;
    @Mock
    private ResourceConfig resourceConfig;
    @Mock
    private HttpServletRequest mRequest;

    private IOSController underTest;

    @Before
    public void setup() {
        when(mRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/mobile-ws"));
        when(mRequest.getContextPath()).thenReturn("/mobile-ws");
        when(iosConfig.getIdentifiers()).thenReturn(toMap("yazinoapp", "SLOTS", "blackjack", "BLACKJACK"));

        underTest = new IOSController(lightstreamerConfig, facebookConfig, iosConfig,
                gamesConfig, tapjoyConfig, resourceConfig);
    }

    @Test
    public void shouldReturnNullWhenNoSuchGame() throws Exception {
        assertNull(underTest.handleBootstrapRequest("Foo", "1.0", "config"));
        assertNull(underTest.handleResourceRequest("Foo", "1.0", "config"));
    }

    @Test
    public void shouldReturnCorrectPathForGame() throws Exception {
        assertEquals("ios/bootstrap/shared/1.0/config", underTest.handleBootstrapRequest("yazinoapp", "1.0", "config").getViewName());
        assertEquals("ios/resources/shared/1.0/config", underTest.handleResourceRequest("blackjack", "1.0", "config").getViewName());
    }

    @Test
    public void shouldReturnCorrectPathWithGameMapping() throws Exception {
        ModelAndView bootstrapMav = underTest.handleBootstrapRequest("YazinoApp", "1.0", "config");
        assertEquals("ios/bootstrap/shared/1.0/config", bootstrapMav.getViewName());
        ModelAndView resourceMav = underTest.handleResourceRequest("YazinoApp", "1.0", "config");
        assertEquals("ios/resources/shared/1.0/config", resourceMav.getViewName());

        assertEquals("SLOTS", bootstrapMav.getModelMap().get(GAME_TYPE));
        assertEquals("SLOTS", resourceMav.getModelMap().get(GAME_TYPE));
    }

    @Test
    public void shouldReturnWithGameType() throws Exception {
        assertNotNull(modelFromBootstrapRequest("YazinoApp", "1.0", "config").get(GAME_TYPE));
        assertNotNull(modelFromResourceRequest("Blackjack", "1.0", "config").get(GAME_TYPE));
    }

    @Test
    public void shouldReturnWithLightstreamerConfig() throws Exception {
        assertSame(lightstreamerConfig, modelFromBootstrapRequest("YazinoApp", "1.0", "config").get(LIGHTSTREAMER));
        assertSame(lightstreamerConfig, modelFromResourceRequest("Blackjack", "1.0", "config").get(LIGHTSTREAMER));
    }

    @Test
    public void shouldReturnWithFacebookConfig() throws Exception {
        assertSame(facebookConfig, modelFromBootstrapRequest("Blackjack", "1.0", "config").get(FACEBOOK));
        assertSame(facebookConfig, modelFromResourceRequest("Blackjack", "1.0", "config").get(FACEBOOK));
    }

    @Test
    public void shouldReturnWithResourcesConfig() throws Exception {
        assertNotNull(modelFromBootstrapRequest("YazinoApp", "1.0", "config").get(RESOURCES));
        assertNotNull(modelFromResourceRequest("YazinoApp", "1.0", "config").get(RESOURCES));
    }

    @Test
    public void shouldReturnWithIOSConfig() throws Exception {
        assertSame(iosConfig, modelFromBootstrapRequest("YazinoApp", "1.0", "config").get(IOS_CONFIG));
        assertSame(iosConfig, modelFromResourceRequest("YazinoApp", "1.0", "config").get(IOS_CONFIG));
    }

    @Test
    public void shouldReturnWithGamesConfig() throws Exception {
        assertSame(gamesConfig, modelFromBootstrapRequest("YazinoApp", "1.0", "config").get(GAMES_CONFIG));
        assertSame(gamesConfig, modelFromResourceRequest("Blackjack", "1.0", "config").get(GAMES_CONFIG));
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAllowNullLightstreamerConfig() throws Exception {
        underTest = new IOSController(null, facebookConfig, iosConfig,
                gamesConfig, tapjoyConfig, resourceConfig);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAllowNullFacebookConfig() throws Exception {
        underTest = new IOSController(lightstreamerConfig, null, iosConfig,
                gamesConfig, tapjoyConfig, resourceConfig);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAllowNullIOSConfig() throws Exception {
        underTest = new IOSController(lightstreamerConfig, facebookConfig, null,
                gamesConfig, tapjoyConfig, resourceConfig);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAllowNullGamesConfig() throws Exception {
        underTest = new IOSController(lightstreamerConfig, facebookConfig, iosConfig,
                null, tapjoyConfig, resourceConfig);
    }

    private ModelMap modelFromBootstrapRequest(String game, String version, String resource) {
        return underTest.handleBootstrapRequest(game, version, resource).getModelMap();
    }

    private ModelMap modelFromResourceRequest(String game, String version, String resource) {
        return underTest.handleResourceRequest(game, version, resource).getModelMap();
    }

    private static Map<String, String> toMap(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            String key = pairs[i];
            String value = pairs[i + 1];
            map.put(key, value);
        }
        return map;
    }

}
