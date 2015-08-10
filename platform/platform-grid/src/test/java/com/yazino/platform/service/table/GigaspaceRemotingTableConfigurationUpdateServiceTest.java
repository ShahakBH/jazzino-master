package com.yazino.platform.service.table;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.model.table.Countdown;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.plugin.GamePluginManager;
import com.yazino.platform.repository.table.*;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.platform.util.JsonHelper;
import com.yazino.platform.util.Visitor;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import com.yazino.game.api.GameType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.model.table.TableControlMessageType.SHUTDOWN;
import static java.math.BigDecimal.valueOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class GigaspaceRemotingTableConfigurationUpdateServiceTest {

    @Mock
    private TableRepository tableRepository;
    @Mock
    private CountdownRepository countdownRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private GamePluginManager gamePluginManager;
    @Mock
    private DocumentDispatcher documentDispatcher;
    @Mock
    private GameVariationRepository gameTemplateRepository;
    @Mock
    private GameConfigurationRepository gameConfigurationRepository;

    private final JsonHelper jsonHelper = new JsonHelper();

    private GigaspaceRemotingTableConfigurationUpdateService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(1000);

        underTest = new GigaspaceRemotingTableConfigurationUpdateService(
                gameRepository, countdownRepository, tableRepository, gameConfigurationRepository, gameTemplateRepository, gamePluginManager, documentDispatcher);
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void aNullGameTypeIsRejected() {
        underTest.setAvailabilityFor(null, false);
    }

    @Test
    public void settingAvailabilityForAGameTypeDelegatesToTheRepository() {
        underTest.setAvailabilityFor("aGameType", true);

        verify(gameRepository).setGameAvailable("aGameType", true);
    }

    @Test
    public void publishCountdownIntoSpaceAndPublishToQueueSpecifiedInContext() {
        Set<BigDecimal> tableIds = newHashSet(BigDecimal.ONE);
        when(tableRepository.findAllLocalTablesWithPlayers()).thenReturn(tableIds);
        Countdown countdown = new Countdown("ALL", new Date().getTime());
        Document document = new Document(DocumentType.COUNTDOWN.name(), jsonHelper.serialize(countdown), new HashMap<String, String>());

        underTest.publishCountdownForAllGames(countdown.getCountdown());
        verify(countdownRepository).publishIntoSpace(countdown);
        verify(documentDispatcher).dispatch(document, Collections.<BigDecimal>emptySet());
    }

    @Test
    public void publishCountdownForGameIntoSpaceAndPublishToQueueSpecifiedInContext() {
        Set<BigDecimal> tableIds = newHashSet(BigDecimal.ONE);
        when(tableRepository.findAllLocalTablesWithPlayers()).thenReturn(tableIds);
        Countdown countdown = new Countdown("aGameType", new Date().getTime());
        Document document = new Document(DocumentType.COUNTDOWN.name(), jsonHelper.serialize(countdown), new HashMap<String, String>());

        underTest.publishCountdownForGameType(countdown.getCountdown(), countdown.getId());
        verify(countdownRepository).publishIntoSpace(countdown);
        verify(documentDispatcher).dispatch(document, Collections.<BigDecimal>emptySet());
    }

    @Test
    public void removeCountdownFromCommunitySpace() {
        final Countdown countdown = new Countdown("ALL", 0L);
        underTest.stopCountdown(countdown.getId());

        verify(countdownRepository).removeCountdownFromSpace(countdown);
        verify(documentDispatcher).dispatch(Matchers.<Document>anyObject());
    }

    @Test
    public void refreshingGameTemplatesDelegatesToTheRepository() {
        underTest.refreshTemplates();

        verify(gameTemplateRepository).refreshAll();
    }

    @Test
    public void shouldRefreshGameConfigurationRepository() throws Exception {
        underTest.refreshGameConfigurations();

        verify(gameConfigurationRepository).refreshAll();
    }

    @Test
    public void disableAndShutdownDisablesAllGameTypes() {
        when(gameRepository.getAvailableGameTypes()).thenReturn(newHashSet(gameTypeFor("BLACKJACK"), gameTypeFor("ROULETTE")));

        underTest.disableAndShutdownAllGames();

        verify(gameRepository).setGameAvailable("BLACKJACK", false);
        verify(gameRepository).setGameAvailable("ROULETTE", false);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void disableAndShutdownAllVisitsAllTablesAndInvokesAShutdownControlMessage() {
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Visitor<Table> tableVisitor = (Visitor<Table>) invocation.getArguments()[0];
                tableVisitor.visit(aTable(valueOf(324)));
                tableVisitor.visit(aTable(valueOf(327)));
                tableVisitor.visit(aTable(valueOf(329)));
                return null;
            }
        }).when(tableRepository).visitAllLocalTables(any(Visitor.class));

        underTest.disableAndShutdownAllGames();

        verify(tableRepository).sendControlMessage(valueOf(324), SHUTDOWN);
        verify(tableRepository).sendControlMessage(valueOf(327), SHUTDOWN);
        verify(tableRepository).sendControlMessage(valueOf(329), SHUTDOWN);
    }

    private GameTypeInformation gameTypeFor(final String type) {
        return new GameTypeInformation(new GameType(type, type + "Name", newHashSet("boo")), true);
    }

    private Table aTable(final BigDecimal tableId) {
        final Table table = new Table();
        table.setTableId(tableId);
        return table;
    }

}
