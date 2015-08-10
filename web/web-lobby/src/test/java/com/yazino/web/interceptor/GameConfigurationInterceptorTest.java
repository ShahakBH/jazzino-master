package com.yazino.web.interceptor;

import com.yazino.platform.table.GameConfiguration;
import com.yazino.web.service.GameConfigurationRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GameConfigurationInterceptorTest {

    public static final GameConfiguration BLACKJACK = gc("BLACKJACK", "bj", 2);
    private List<GameConfiguration> gameConfigurations;

    @Mock
    private GameConfigurationRepository gameConfigurationRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        gameConfigurations = new ArrayList<GameConfiguration>(asList(BLACKJACK));
    }

    @Test
    public void shouldIncludeGameConfigurationsOnTheModel() throws Exception {
        when(gameConfigurationRepository.findAll()).thenReturn(gameConfigurations);
        GameConfigurationInterceptor underTest = new GameConfigurationInterceptor(gameConfigurationRepository);
        ModelAndView modelAndView = new ModelAndView();

        underTest.postHandle(request, response, null, modelAndView);

        assertThat(gameConfigurations, is(equalTo(modelAndView.getModel().get("gameConfigurations"))));
    }

    private static GameConfiguration gc(final String gameType, final String shortName, final int order, final String... aliases) {
        return new GameConfiguration(gameType, shortName, "dn", new HashSet<String>(asList(aliases)), order);
    }
}
