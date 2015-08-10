package com.yazino.platform.service.tournament;

import com.yazino.platform.grid.Executor;
import com.yazino.platform.grid.ExecutorTestUtils;
import com.yazino.platform.repository.table.ClientRepository;
import com.yazino.platform.repository.table.TableRepository;
import com.yazino.platform.service.table.InternalTableService;
import com.yazino.platform.table.TableException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.PlayerAtTableInformation;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceTournamentTableServiceTest {

    private static final String GAME_TYPE = "BLACKJACK";
    private static final BigDecimal TEMPLATE_ID = BigDecimal.valueOf(2342534L);
    private static final String CLIENT_ID = "WhiteKnight";
    private static final String PARTNER_ID = "EEE";
    private static final String TABLE_NAME = "HotHotHot";

    @Mock
    private InternalTableService internalTableService;
    @Mock
    private TableRepository tableGlobalRepository;
    @Mock
    private TableRepository injectedTableRepository;
    @Mock
    private ClientRepository clientRepository;

    private GigaspaceTournamentTableService underTest;
    private Executor executor;

    @Before
    public void setUp() {
        final Map<String, Object> injectedServices = newHashMap();
        injectedServices.put("tableRepository", injectedTableRepository);
        executor = ExecutorTestUtils.mockExecutorWith(3, injectedServices);

        underTest = new GigaspaceTournamentTableService(internalTableService, tableGlobalRepository, clientRepository, executor);
    }

    @Test(expected = NullPointerException.class)
    public void serviceCannotBeCreatedWithANullTableService() {
        new GigaspaceTournamentTableService(null, tableGlobalRepository, clientRepository, executor);
    }

    @Test(expected = NullPointerException.class)
    public void serviceCannotBeCreatedWithANullTableRepository() {
        new GigaspaceTournamentTableService(internalTableService, null, clientRepository, executor);
    }

    @Test(expected = NullPointerException.class)
    public void serviceCannotBeCreatedWithANullClientRepository() {
        new GigaspaceTournamentTableService(internalTableService, tableGlobalRepository, null, executor);
    }

    @Test(expected = NullPointerException.class)
    public void serviceCannotBeCreatedWithANullExecutor() {
        new GigaspaceTournamentTableService(internalTableService, tableGlobalRepository, clientRepository, null);
    }

    @Test
    public void shouldCreateTable() throws TableException {
        when(internalTableService.createTournamentTable(GAME_TYPE, TEMPLATE_ID, CLIENT_ID, TABLE_NAME))
                .thenReturn(BigDecimal.valueOf(10))
                .thenReturn(BigDecimal.valueOf(20))
                .thenReturn(BigDecimal.valueOf(30));

        final List<BigDecimal> tableIds = underTest.createTables(3, GAME_TYPE,
                TEMPLATE_ID, CLIENT_ID, PARTNER_ID, TABLE_NAME);

        assertEquals(3, tableIds.size());
        assertTrue(tableIds.contains(BigDecimal.valueOf(10)));
        assertTrue(tableIds.contains(BigDecimal.valueOf(20)));
        assertTrue(tableIds.contains(BigDecimal.valueOf(30)));
    }

    @Test
    public void removeShouldSendUnloadMessages() {
        final Set<BigDecimal> tableIds = newHashSet(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3));

        underTest.removeTables(tableIds);

        verify(internalTableService).unload(BigDecimal.valueOf(1));
        verify(internalTableService).unload(BigDecimal.valueOf(2));
        verify(internalTableService).unload(BigDecimal.valueOf(3));
    }

    @Test
    public void gettingActivePlayersShouldReturnAnEmptySetForANullListOfTableIds() {
        assertThat(underTest.getActivePlayers(null), is(equalTo(Collections.<PlayerAtTableInformation>emptySet())));
    }

    @Test
    public void gettingActivePlayersShouldDelegateToTheActivePlayerService() {
        when(injectedTableRepository.findLocalActivePlayers(newHashSet(BigDecimal.valueOf(1), BigDecimal.valueOf(2))))
                .thenReturn(newHashSet(playerAtTableInfoFor(1)))
                .thenReturn(new HashSet<PlayerAtTableInformation>())
                .thenReturn(newHashSet(playerAtTableInfoFor(2)));

        final Set<PlayerAtTableInformation> activePlayers = underTest.getActivePlayers(
                newHashSet(BigDecimal.valueOf(1), BigDecimal.valueOf(2)));

        assertThat(activePlayers, is(equalTo((Set) newHashSet(playerAtTableInfoFor(1), playerAtTableInfoFor(2)))));
    }

    @Test
    public void countingOpenTablesShouldReturnZeroForANullListOfTableIds() {
        assertThat(underTest.getOpenTableCount(null), is(equalTo(0)));
    }

    @Test
    public void countingOpenTablesShouldDelegateToTheTableCountService() {
        when(tableGlobalRepository.countOpenTables(newHashSet(BigDecimal.valueOf(1), BigDecimal.valueOf(2)))).thenReturn(3);

        final int openTableCount = underTest.getOpenTableCount(newHashSet(BigDecimal.valueOf(1), BigDecimal.valueOf(2)));

        assertThat(openTableCount, is(equalTo(3)));
    }

    private PlayerAtTableInformation playerAtTableInfoFor(final int i) {
        return new PlayerAtTableInformation(new GamePlayer(BigDecimal.valueOf(i), null, "player" + i), Collections.<String, String>emptyMap());
    }
}
