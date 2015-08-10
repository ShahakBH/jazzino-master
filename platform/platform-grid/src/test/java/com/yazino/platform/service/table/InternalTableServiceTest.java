package com.yazino.platform.service.table;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableControlMessageType;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.table.ClientRepository;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.repository.table.GameVariationRepository;
import com.yazino.platform.repository.table.TableRepository;
import com.yazino.platform.table.*;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameType;
import com.yazino.game.api.PlayerAtTableInformation;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.collect.Sets.newHashSet;
import static java.math.BigDecimal.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InternalTableServiceTest {

    private static final GameType GAME_TYPE = new GameType("BLACKJACK", "Blackjack", Collections.<String>emptySet());
    private static final String GAME_VARIATION_NAME = "testTemplate1";
    private static final String CLIENT_ID = "testClient";
    private static final BigDecimal TABLE_ID = valueOf(344234);
    private static final BigDecimal GAME_VARIATION_ID = valueOf(45345);
    private static final String TABLE_NAME = "myLittleTable";
    private static final BigDecimal OWNER_ID = new BigDecimal(123);

    private final Map<String, String> expectedProperties = new ConcurrentHashMap<String, String>();

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private TableRepository tableGlobalRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameRules gameRules;
    @Mock
    private GameVariationRepository gameVariationRepository;
    @Mock
    private SequenceGenerator sequenceGenerator;
    @Mock
    private InternalTableService internalTableService;

    private Client client;
    private InternalTableService underTest;


    @Before
    public void setUp() {
        underTest = new InternalTableService(sequenceGenerator, clientRepository, tableGlobalRepository, gameRepository, gameVariationRepository);

        client = new Client(CLIENT_ID);
        client.setGameType(GAME_TYPE.getId());
        client.setNumberOfSeats(5);
        client.setClientFile("aClientFile");

        expectedProperties.put("Hello", "Fellow");

        when(clientRepository.findById(CLIENT_ID)).thenReturn(client);
        when(clientRepository.findAll(GAME_TYPE.getId())).thenReturn(new Client[]{client});

        when(sequenceGenerator.next()).thenReturn(TABLE_ID);

        when(gameRepository.isGameAvailable(GAME_TYPE.getId())).thenReturn(true);
        when(gameRepository.getGameTypeFor(GAME_TYPE.getId())).thenReturn(GAME_TYPE);
        when(gameRepository.getGameRules(GAME_TYPE.getId())).thenReturn(gameRules);

        when(gameVariationRepository.getIdForName(GAME_VARIATION_NAME, GAME_TYPE.getId())).thenReturn(GAME_VARIATION_ID);
        GameVariation gameVariation = new GameVariation(GAME_VARIATION_ID, GAME_TYPE.getId(), GAME_VARIATION_NAME, expectedProperties);
        when(gameVariationRepository.findById(GAME_VARIATION_ID)).thenReturn(gameVariation);
        when(gameVariationRepository.variationsFor(GAME_TYPE.getId())).thenReturn(newHashSet(gameVariation));
    }

    @Test
    public void aPublicTableShouldBeCreatedWithTheGivenName()
            throws WalletServiceException, TableException {
        underTest.createPublicTable(GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, TABLE_NAME, Collections.<String>emptySet());

        assertThat(theCreatedTable(), is(equalTo(aPublicTableNamed(TABLE_NAME))));
    }

    @Test
    public void aPublicTableShouldBeCreatedAndTheSummaryReturned()
            throws WalletServiceException, TableException {
        final TableSummary table = underTest.createPublicTable(GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, TABLE_NAME, Collections.<String>emptySet());

        assertThat(table.getId(), is(equalTo(TABLE_ID)));
        assertThat(table.getGameType(), is(equalTo(GAME_TYPE)));
        assertThat(table.getTemplateName(), is(equalTo(GAME_VARIATION_NAME)));
        assertThat(table.getClientId(), is(equalTo(CLIENT_ID)));
        assertThat(table.getName(), is(equalTo(TABLE_NAME)));
        assertThat(table.getClientFile(), is(equalTo(client.getClientFile())));
        assertThat(table.getStatus(), is(equalTo(TableStatus.open)));
        assertThat(table.getMonitoringMessage(), is(nullValue()));
    }

    @Test
    public void aPublicTableUsingTheDefaultTemplateAndVariationShouldBeCreatedAndTheSummaryReturned()
            throws WalletServiceException, TableException {
        final TableSummary table = underTest.createPublicTable(GAME_TYPE.getId(), TABLE_NAME, Collections.<String>emptySet());

        assertThat(table.getId(), is(equalTo(TABLE_ID)));
        assertThat(table.getGameType(), is(equalTo(GAME_TYPE)));
        assertThat(table.getTemplateName(), is(equalTo(GAME_VARIATION_NAME)));
        assertThat(table.getClientId(), is(equalTo(CLIENT_ID)));
        assertThat(table.getName(), is(equalTo(TABLE_NAME)));
        assertThat(table.getClientFile(), is(equalTo(client.getClientFile())));
        assertThat(table.getStatus(), is(equalTo(TableStatus.open)));
        assertThat(table.getMonitoringMessage(), is(nullValue()));
    }

    @Test(expected = TableException.class)
    public void aPublicTableUsingTheDefaultTemplateAndVariationShouldThrowATableExceptionWhenNoClientIsAvailable()
            throws WalletServiceException, TableException {
        reset(clientRepository);
        when(clientRepository.findAll(GAME_TYPE.getId())).thenReturn(new Client[0]);

        underTest.createPublicTable(GAME_TYPE.getId(), TABLE_NAME, Collections.<String>emptySet());
    }

    @Test(expected = TableException.class)
    public void aPublicTableUsingTheDefaultTemplateAndVariationShouldThrowATableExceptionWhenNoVariationIsAvailable()
            throws WalletServiceException, TableException {
        reset(gameVariationRepository);
        when(gameVariationRepository.variationsFor(GAME_TYPE.getId())).thenReturn(new HashSet<GameVariation>());

        underTest.createPublicTable(GAME_TYPE.getId(), TABLE_NAME, Collections.<String>emptySet());
    }

    @Test
    public void aPublicTableShouldNotBeCreatedWhenTheClientIsForADifferentGameType()
            throws WalletServiceException, TableException {
        reset(clientRepository);
        client = new Client(CLIENT_ID);
        client.setGameType("anotherGameType");
        when(clientRepository.findById(CLIENT_ID)).thenReturn(client);

        try {
            underTest.createPublicTable(GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, TABLE_NAME, Collections.<String>emptySet());
            fail("Creation succeeded");
        } catch (TableException e) {
            assertThat(e.getResult(), is(equalTo(TableOperationResult.INVALID_CLIENT_FOR_GAMETYPE)));
        }
    }

    @Test
    public void aPrivateTableShouldBeCreatedWithTheGivenName()
            throws WalletServiceException, TableException {
        final TableSummary table = underTest.createPrivateTableForPlayer(
                GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, TABLE_NAME, OWNER_ID);

        assertThat(theCreatedTable(), is(equalTo(aPrivateTableNamed(TABLE_NAME))));
        assertThat(table.getId(), is(equalTo(TABLE_ID)));
    }

    @Test
    public void aPrivateTableShouldBeCreatedAndReturnTheSummaryReturned()
            throws WalletServiceException, TableException {
        final TableSummary table = underTest.createPrivateTableForPlayer(
                GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, TABLE_NAME, OWNER_ID);

        assertThat(table.getId(), is(equalTo(TABLE_ID)));
        assertThat(table.getGameType().getId(), is(equalTo("BLACKJACK")));
        assertThat(table.getTemplateName(), is(equalTo(GAME_VARIATION_NAME)));
        assertThat(table.getClientId(), is(equalTo(CLIENT_ID)));
        assertThat(table.getName(), is(equalTo(TABLE_NAME)));
        assertThat(table.getClientFile(), is(equalTo(client.getClientFile())));
        assertThat(table.getStatus(), is(equalTo(TableStatus.open)));
        assertThat(table.getMonitoringMessage(), is(nullValue()));
    }

    @Test
    public void aPublicTableShouldBeCreatedWithTheDefaultNameWhereNoneIsSupplied()
            throws WalletServiceException, TableException {
        final TableSummary table = underTest.createPublicTable(GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, null, Collections.<String>emptySet());

        assertThat(theCreatedTable(), is(equalTo(aPublicTableNamed(GAME_VARIATION_NAME + " " + TABLE_ID))));
        assertThat(table.getName(), is(equalTo(GAME_VARIATION_NAME + " " + TABLE_ID)));
    }

    @Test
    public void aPublicTableShouldBeCreatedWithNoTagsWhereAnEmptySetIsSupplied()
            throws WalletServiceException, TableException {
        final TableSummary table = underTest.createPublicTable(GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, null, Collections.<String>emptySet());

        assertThat(table.getTags(), is(empty()));
    }

    @Test
    public void aPublicTableShouldBeCreatedWithNoTagsWhereAnNullSetIsSupplied()
            throws WalletServiceException, TableException {
        final TableSummary table = underTest.createPublicTable(GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, null, null);

        assertThat(table.getTags(), is(empty()));
    }

    @Test
    public void aPublicTableShouldBeCreatedWithTheSuppliedTagsWhereOneOrMoreTagsAreSupplied()
            throws WalletServiceException, TableException {
        final TableSummary table = underTest.createPublicTable(GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, null, newHashSet("aTag", "anotherTag"));

        assertThat(table.getTags(), hasItems("aTag", "anotherTag"));
        assertThat(table.getTags().size(), is(equalTo(2)));
    }

    @Test
    public void aTournamentTableShouldBeCreatedWithTheDefaultName()
            throws WalletServiceException, TableException {
        final BigDecimal tableId = underTest.createTournamentTable(GAME_TYPE.getId(), GAME_VARIATION_ID, CLIENT_ID, null);

        assertThat(theCreatedTable(), is(equalTo(aTournamentTableNamed(GAME_VARIATION_NAME + " " + TABLE_ID))));
        assertThat(tableId, is(equalTo(TABLE_ID)));
    }

    @Test
    public void anUnavailableGameTypeShouldBeRejected() {
        reset(gameRepository);
        when(gameRepository.isGameAvailable(GAME_TYPE.getId())).thenReturn(false);

        try {
            underTest.createPublicTable(GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, null, Collections.<String>emptySet());
            fail("Expected exception not thrown");

        } catch (TableException e) {
            assertThat(e, hasResult(TableOperationResult.GAME_TYPE_UNAVAILABLE));

        } catch (Exception e) {
            fail("Unexpected exception thrown: " + e);
        }
    }

    @Test
    public void anInvalidTemplateNameShouldBeRejected() throws TableException {
        reset(gameVariationRepository);
        when(gameVariationRepository.getIdForName(GAME_VARIATION_NAME, GAME_TYPE.getId())).thenReturn(null);

        try {
            underTest.createPublicTable(GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, null, Collections.<String>emptySet());
            fail("Expected exception not thrown");

        } catch (TableException e) {
            assertThat(e, hasResult(TableOperationResult.UNKNOWN_TEMPLATE));

        } catch (Exception e) {
            fail("Unexpected exception thrown: " + e);
        }
    }

    @Test
    public void anInvalidClientIdShouldBeIgnoredIfThereIsOnlyOneClientForTheGameTYpe() throws TableException {
        reset(clientRepository);
        when(clientRepository.findAll(GAME_TYPE.getId())).thenReturn(new Client[]{
                new Client(CLIENT_ID, 5, client.getClientFile(), GAME_TYPE.getId(), Collections.<String, String>emptyMap())});

        final TableSummary table = underTest.createPublicTable(
                GAME_TYPE.getId(), GAME_VARIATION_NAME, "anInvalidClientId", null, Collections.<String>emptySet());

        assertThat(table.getId(), is(equalTo(TABLE_ID)));
        assertThat(table.getGameType(), is(equalTo(GAME_TYPE)));
        assertThat(table.getTemplateName(), is(equalTo(GAME_VARIATION_NAME)));
        assertThat(table.getClientId(), is(equalTo(CLIENT_ID)));
        assertThat(table.getName(), is(startsWith(GAME_VARIATION_NAME)));
        assertThat(table.getClientFile(), is(equalTo(client.getClientFile())));
        assertThat(table.getStatus(), is(equalTo(TableStatus.open)));
        assertThat(table.getMonitoringMessage(), is(nullValue()));
    }

    @Test
    public void anInvalidClientIdShouldBeRejectedIfMoreThanOneOptionIsAvailable() {
        reset(clientRepository);
        when(clientRepository.findAll(GAME_TYPE.getId())).thenReturn(
                new Client[]{new Client(CLIENT_ID), new Client(CLIENT_ID + "2")});

        try {
            underTest.createPublicTable(GAME_TYPE.getId(), GAME_VARIATION_NAME, "anInvalidClientId", null, Collections.<String>emptySet());
            fail("Expected exception not thrown");

        } catch (TableException e) {
            assertThat(e, hasResult(TableOperationResult.UNKNOWN_CLIENT));

        } catch (Exception e) {
            fail("Unexpected exception thrown: " + e);
        }
    }

    @Test
    public void aPrivateTableCreationShouldBeRejectedIfATableAlreadyExistsForTheGameTypeAndPlayer()
            throws TableException {
        when(tableGlobalRepository.findTableByGameTypeAndPlayer(GAME_TYPE.getId(), OWNER_ID))
                .thenReturn(aPublicTableNamed("BJ").summarise(gameRules));

        try {
            underTest.createPrivateTableForPlayer(
                    GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, TABLE_NAME, OWNER_ID);
            fail("Expected exception not thrown");

        } catch (TableException e) {
            assertThat(e, hasResult(TableOperationResult.TABLE_ALREADY_EXISTS_FOR_GAMETYPE));

        } catch (Exception e) {
            fail("Unexpected exception thrown: " + e);
        }
    }

    @Test(expected = NullPointerException.class)
    public void aNullGameTypeShouldBeRejected() throws TableException {
        underTest.createPublicTable(null, GAME_VARIATION_NAME, CLIENT_ID, null, Collections.<String>emptySet());
    }

    @Test
    public void closeRequestsShouldBeWrittenToTheGlobalSpace() {
        underTest.closeTable(TABLE_ID);

        verify(tableGlobalRepository).sendControlMessage(TABLE_ID, TableControlMessageType.CLOSE);
    }

    @Test(expected = NullPointerException.class)
    public void closeRequestsWithNullTableIdsAreRejected() {
        underTest.closeTable(null);
    }

    @Test
    public void unloadRequestsShouldGenerateAControlMessageToTheGlobalSpace() {
        underTest.unload(TABLE_ID);

        verify(tableGlobalRepository).sendControlMessage(TABLE_ID, TableControlMessageType.UNLOAD);
    }

    @Test(expected = NullPointerException.class)
    public void forcingANewGameRejectsNullTableIds() {
        final BigDecimal playerId = valueOf(12);
        final PlayerAtTableInformation playerInfo = new PlayerAtTableInformation(
                new GamePlayer(playerId, null, "aPlayer"), singletonMap("aProperty", "aValue"));

        underTest.forceNewGame(null, asList(playerInfo), valueOf(113), "aClientId", singletonMap(playerId, valueOf(104)));
    }

    @Test
    public void forcingANewGameDelegatesToTheGlobalRepository() {
        final BigDecimal playerId = valueOf(12);
        final PlayerAtTableInformation playerInfo = new PlayerAtTableInformation(
                new GamePlayer(playerId, null, "aPlayer"), singletonMap("aProperty", "aValue"));

        underTest.forceNewGame(valueOf(18), asList(playerInfo), valueOf(113), "aClientId", singletonMap(playerId, valueOf(104)));

        verify(tableGlobalRepository).forceNewGame(valueOf(18), asList(playerInfo), valueOf(113), "aClientId", singletonMap(playerId, valueOf(104)));
    }

    @Test
    public void findingAVariationByIdShouldFetchItFromTheRepository() {
        final GameVariation expectedGameVariation = new GameVariation(
                GAME_VARIATION_ID, GAME_TYPE.getId(), GAME_VARIATION_NAME, expectedProperties);
        when(gameVariationRepository.findById(GAME_VARIATION_ID)).thenReturn(expectedGameVariation);

        final GameVariation gameVariation = underTest.getGameVariation(GAME_VARIATION_ID);

        assertThat(gameVariation, is(equalTo(expectedGameVariation)));
    }

    @Test
    public void findingANonExistentVariationByIdShouldReturnNull() {
        when(gameVariationRepository.findById(GAME_VARIATION_ID)).thenReturn(null);

        final GameVariation gameVariation = underTest.getGameVariation(GAME_VARIATION_ID);

        assertThat(gameVariation, is(nullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void findingAVariationWithANullIdShouldThrowAnException() {
        underTest.getGameVariation(null);
    }

    private Table aTournamentTableNamed(final String tableName) {
        final Table table = aTableNamed(tableName);
        table.setShowInLobby(false);
        table.setTableStatus(TableStatus.closed);
        return table;
    }

    private Table aPublicTableNamed(final String tableName) {
        final Table table = aTableNamed(tableName);
        table.setShowInLobby(true);
        table.setTableStatus(TableStatus.open);
        return table;
    }

    private Table aPrivateTableNamed(final String tableName) {
        final Table table = aTableNamed(tableName);
        table.setShowInLobby(false);
        table.setTableStatus(TableStatus.open);
        table.setOwnerId(OWNER_ID);
        return table;
    }

    private Table aTableNamed(final String tableName) {
        final Table table = new Table(GAME_TYPE, GAME_VARIATION_ID, CLIENT_ID, true);
        table.setTableName(tableName);
        table.setTableId(TABLE_ID);
        table.setClient(client);
        table.setFull(false);
        table.setTableStatus(TableStatus.open);
        table.setTemplateName(GAME_VARIATION_NAME);
        table.setVariationProperties(expectedProperties);
        return table;
    }

    private Table theCreatedTable() {
        final ArgumentCaptor<Table> tableCaptor = ArgumentCaptor.forClass(Table.class);
        verify(tableGlobalRepository).save(tableCaptor.capture());
        return tableCaptor.getValue();
    }

    private Matcher<TableException> hasResult(final TableOperationResult result) {
        return new TypeSafeMatcher<TableException>() {
            @Override
            public boolean matchesSafely(final TableException e) {
                return e.getResult().equals(result);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("had result ").appendValue(result);
            }
        };
    }
}
