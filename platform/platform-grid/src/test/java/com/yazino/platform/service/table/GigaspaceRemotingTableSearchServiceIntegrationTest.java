package com.yazino.platform.service.table;

import com.yazino.platform.model.table.Client;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.processor.table.RandomTableGenerator;
import com.yazino.platform.repository.table.ClientRepository;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.table.PlayerInformation;
import com.yazino.platform.table.TableSearchCriteria;
import com.yazino.platform.table.TableSearchResult;
import com.yazino.platform.table.TableStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameStatus;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaspaceRemotingTableSearchServiceIntegrationTest {

    private final ClientRepository clientRepository = mock(ClientRepository.class);
    private final GameRepository gameRepository = mock(GameRepository.class);
    private final GameRules gameRules = mock(GameRules.class);

    @Autowired
    private GigaSpace gigaSpace;
    private GigaspaceRemotingTableSearchService tableSearchService;

    private final String gameType = "SLOTS";
    private final String variation = "Slots Low";
    private final String clientId = "Default Slots";

    @Before
    public void setup() {
        Client client = new Client(clientId);
        client.setNumberOfSeats(10);
        when(clientRepository.findById(clientId)).thenReturn(client);
        tableSearchService = new GigaspaceRemotingTableSearchService(gigaSpace, clientRepository, gameRepository);
        gigaSpace.clear(new Table());
    }

    @Test
    public void shouldReturnEmptySetWhenNoTables() throws Exception {
        Collection<TableSearchResult> found = tableSearchService.findTables(BigDecimal.TEN, new TableSearchCriteria(gameType, variation,  clientId, Collections.<String>emptySet()));

        assertEquals(0, found.size());
    }

    @Test
    public void shouldReturnOneTableWhenOnlyOneMatchesAndClientAndTemplateAreSpecified() throws Exception {
        RandomTableGenerator generator = new RandomTableGenerator();
        generator.setGameTypes("BLACKJACK", "HISSTERIA", "ROULETTE");
        Set<Table> nonMatching = generator.generateTables(100);

        generator.setGameTypes(gameType);
        generator.setTemplateNames(toMap(gameType, variation));
        generator.setClients(toMap(gameType, clientId));
        generator.setFulls(false);
        generator.setShowInLobbys(true);
        generator.setAvailableForJoinings(true);
        generator.setStatii(TableStatus.open);

        GameStatus gameStatus = mock(GameStatus.class);
        when(gameRepository.getGameRules(anyString())).thenReturn(gameRules);
        when(gameRules.getJoiningDesirability(gameStatus)).thenReturn(45);

        Set<Table> matching = generator.generateTables(1);
        Table generatedTable = matching.iterator().next();
        generatedTable.setTableId(BigDecimal.valueOf(67));
        generatedTable.setCurrentGame(gameStatus);

        Set<Table> all = new HashSet<Table>(nonMatching);
        all.addAll(matching);
        gigaSpace.writeMultiple(all.toArray(new Table[all.size()]));

        assertEquals(101, gigaSpace.count(new Table()));

        Collection<TableSearchResult> found = tableSearchService.findTables(BigDecimal.TEN, new TableSearchCriteria(gameType, variation,clientId, Collections.<String>emptySet()));

        assertEquals(1, found.size());
        TableSearchResult table = found.iterator().next();
        assertEquals(BigDecimal.valueOf(67), table.getTableId());
        assertEquals(10, table.getSpareSeats());
        assertEquals(45, table.getJoiningDesirability());
    }

    @Test
    public void shouldReturnOneTableWhenOnlyOneMatches() throws Exception {
        RandomTableGenerator generator = new RandomTableGenerator();
        generator.setGameTypes("BLACKJACK", "HISSTERIA", "ROULETTE");
        Set<Table> nonMatching = generator.generateTables(100);

        generator.setGameTypes(gameType);
        generator.setTemplateNames(toMap(gameType, variation));
        generator.setClients(toMap(gameType, clientId));
        
        generator.setFulls(false);
        generator.setShowInLobbys(true);
        generator.setAvailableForJoinings(true);
        generator.setStatii(TableStatus.open);

        GameStatus gameStatus = mock(GameStatus.class);
        when(gameRepository.getGameRules(anyString())).thenReturn(gameRules);
        when(gameRules.getJoiningDesirability(gameStatus)).thenReturn(45);

        Set<Table> matching = generator.generateTables(1);
        Table generatedTable = matching.iterator().next();
        generatedTable.setTableId(BigDecimal.valueOf(67));
        generatedTable.setCurrentGame(gameStatus);

        Set<Table> all = new HashSet<Table>(nonMatching);
        all.addAll(matching);
        gigaSpace.writeMultiple(all.toArray(new Table[all.size()]));

        assertEquals(101, gigaSpace.count(new Table()));

        Collection<TableSearchResult> found = tableSearchService.findTables(BigDecimal.TEN, new TableSearchCriteria(gameType,Collections.<String>emptySet()));

        assertEquals(1, found.size());
        TableSearchResult table = found.iterator().next();
        assertEquals(BigDecimal.valueOf(67), table.getTableId());
        assertEquals(10, table.getSpareSeats());
        assertEquals(45, table.getJoiningDesirability());
    }

    @Test
    public void shouldReturnEmptySetWhenPlayerIsPlayingAtOnlyTable() throws Exception {
        RandomTableGenerator generator = new RandomTableGenerator();
        generator.setGameTypes(gameType);
        generator.setTemplateNames(toMap(gameType, variation));
        generator.setClients(toMap(gameType, clientId));
        
        generator.setFulls(false);
        generator.setShowInLobbys(true);
        generator.setAvailableForJoinings(true);
        generator.setStatii(TableStatus.open);

        Set<Table> generated = generator.generateTables(1);

        Table table = generated.iterator().next();
        table.addPlayerToTable(new PlayerInformation(BigDecimal.TEN));

        gigaSpace.writeMultiple(generated.toArray(new Table[generated.size()]));

        Collection<TableSearchResult> found = tableSearchService.findTables(BigDecimal.TEN,
                new TableSearchCriteria(gameType, variation,clientId, Collections.<String>emptySet()));

        assertEquals(0, found.size());
    }

    @Test
    public void shouldReturnEmptySetWhenPlayerIsPlayingAtOnlyTableAndTableIsExcluded() throws Exception {
        RandomTableGenerator generator = new RandomTableGenerator();
        generator.setGameTypes(gameType);
        generator.setTemplateNames(toMap(gameType, variation));
        generator.setClients(toMap(gameType, clientId));
        
        generator.setFulls(false);
        generator.setShowInLobbys(true);
        generator.setAvailableForJoinings(true);
        generator.setStatii(TableStatus.open);
        Set<Table> generated = generator.generateTables(1);
        Table tableWithPlayer = generated.iterator().next();
        tableWithPlayer.addPlayerToTable(new PlayerInformation(BigDecimal.TEN));

        generated = generator.generateTables(1);
        Table excludedTable = generated.iterator().next();
        excludedTable.setTableId(BigDecimal.valueOf(45));


        gigaSpace.writeMultiple(new Table[]{tableWithPlayer, excludedTable});

        Collection<TableSearchResult> found = tableSearchService.findTables(BigDecimal.TEN, new
                TableSearchCriteria(gameType, variation,clientId, Collections.<String>emptySet(), BigDecimal.valueOf(45)));

        assertEquals(0, found.size());
    }

    @Test
    public void shouldReturnTableWhenPlayerIsNotPlayingAtOnlyTable() throws Exception {
        RandomTableGenerator generator = new RandomTableGenerator();
        generator.setGameTypes(gameType);
        generator.setTemplateNames(toMap(gameType, variation));
        generator.setClients(toMap(gameType, clientId));

        generator.setFulls(false);
        generator.setShowInLobbys(true);
        generator.setAvailableForJoinings(true);
        generator.setStatii(TableStatus.open);

        Set<Table> generated = generator.generateTables(1);

        GameStatus gameStatus = mock(GameStatus.class);
        when(gameRepository.getGameRules(anyString())).thenReturn(gameRules);
        when(gameRules.getJoiningDesirability(gameStatus)).thenReturn(45);

        Table first = generated.iterator().next();
        first.addPlayerToTable(new PlayerInformation(BigDecimal.ONE));
        first.setTableId(BigDecimal.valueOf(67));
        first.setCurrentGame(gameStatus);

        gigaSpace.writeMultiple(generated.toArray(new Table[generated.size()]));

        Collection<TableSearchResult> found = tableSearchService.findTables(BigDecimal.TEN,
                new TableSearchCriteria(gameType, variation,clientId, Collections.<String>emptySet()));
        assertEquals(1, found.size());
        TableSearchResult table = found.iterator().next();

        assertEquals(BigDecimal.valueOf(67), table.getTableId());
        assertEquals(10, table.getSpareSeats());
        assertEquals(10, table.getMaxSeats());
        assertEquals(45, table.getJoiningDesirability());
    }

    @Test
    public void shouldReturnAllMatchingTablesWhenManyTablesExist() throws Exception {
        RandomTableGenerator generator = new RandomTableGenerator();
        generator.setGameTypes("BLACKJACK", "HISSTERIA", "ROULETTE");
        Set<Table> nonMatching = generator.generateTables(100);

        generator.setGameTypes(gameType);
        generator.setTemplateNames(toMap(gameType, variation));
        generator.setClients(toMap(gameType, clientId));

        generator.setFulls(false);
        generator.setShowInLobbys(true);
        generator.setAvailableForJoinings(true);
        generator.setStatii(TableStatus.open);
        generator.setTags("tag1", "tag2", "tag3");

        Set<Table> matching = generator.generateTables(12);

        Set<Table> all = new HashSet<Table>(nonMatching);
        all.addAll(matching);
        assertEquals(112, all.size());

        gigaSpace.writeMultiple(all.toArray(new Table[all.size()]));

        Collection<TableSearchResult> found = tableSearchService.findTables(BigDecimal.TEN, new TableSearchCriteria(gameType, variation, clientId, Collections.<String>emptySet()));
        assertEquals(12, found.size());
    }

    @Test
    public void shouldReturnMatchingTablesWhenTagsAreSpecified() throws Exception {
        RandomTableGenerator generator = new RandomTableGenerator();
        generator.setGameTypes("BLACKJACK", "HISSTERIA", "ROULETTE");
        generator.setGameTypes(gameType);
        generator.setTemplateNames(toMap(gameType, variation));
        generator.setClients(toMap(gameType, clientId));

        generator.setFulls(false);
        generator.setShowInLobbys(true);
        generator.setAvailableForJoinings(true);
        generator.setStatii(TableStatus.open);
        generator.setTags("tag1", "tag2", "tag3");

        Set<Table> generated = generator.generateTables(12);
        Table firstTable = generated.iterator().next();
        firstTable.setTags(newHashSet("tag4"));
        gigaSpace.writeMultiple(generated.toArray(new Table[generated.size()]));

        Collection<TableSearchResult> found = tableSearchService.findTables(BigDecimal.TEN, new TableSearchCriteria(gameType, variation,  clientId, newHashSet("tag4")));
        assertThat(found.size(), is(equalTo(1)));
        assertThat(found.iterator().next().getTableId(), is(equalTo(firstTable.getTableId())));
    }

    @Test
    public void shouldReturnEmptyWhenOnlyTableIsExcluded() throws Exception {
        TableSearchCriteria searchCriteria = new TableSearchCriteria(gameType, variation,  clientId, Collections.<String>emptySet(), BigDecimal.valueOf(80));

        RandomTableGenerator generator = new RandomTableGenerator();
        generator.setGameTypes(gameType);
        generator.setTemplateNames(toMap(gameType, variation));
        generator.setClients(toMap(gameType, clientId));

        generator.setFulls(false);
        generator.setShowInLobbys(true);
        generator.setAvailableForJoinings(true);
        generator.setStatii(TableStatus.open);

        Set<Table> generated = generator.generateTables(1);

        Table first = generated.iterator().next();
        first.setTableId(BigDecimal.valueOf(80));

        Collection<TableSearchResult> found = tableSearchService.findTables(BigDecimal.TEN, searchCriteria);
        assertEquals(0, found.size());
    }


    private static Map<String, String[]> toMap(String key, String... values) {
        Map<String, String[]> map = new HashMap<String, String[]>();
        map.put(key, values);
        return map;
    }

}
