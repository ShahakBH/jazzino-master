package com.yazino.platform.processor.table;


import com.gigaspaces.client.ReadModifiers;
import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.TableStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.*;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class TemplateVsSQLIntegrationTest {

    private static final Random random = new Random();
    private static final int numberOfTables = 100;

    private static final String[] gameTypes = {"BLACKJACK", "SLOTS", "TEXAS_HOLDEM", "ROULETTE", "HISSTERIA"};
    private static final Map<String, String[]> templateNames = constructTemplateNames();
    private static final Map<String, String[]> clients = constructClients();

    private static final Set<Table> tables = new HashSet<Table>();

    private final String gameType = "BLACKJACK";
    private final String templateName = "Slots Low";
    private final String partnerId = "YAZINO";
    private final String clientId = "Default Slots";
    private final TableStatus tableStatus = TableStatus.open;
    private final boolean full = false;
    private final boolean showInLobby = true;
    private final boolean availableForPlayersJoining = true;

    private final int totalRequests = 500;

    @Autowired
    private GigaSpace tableSpace;


    @BeforeClass
    public static void createTables() {
        for (int i = 0; i < numberOfTables; i++) {
            int id = random.nextInt();
            String gameType = gameTypes[random.nextInt(gameTypes.length)];
            String[] templatesForGameType = templateNames.get(gameType);
            String template = templatesForGameType[random.nextInt(templatesForGameType.length)];

            String[] clientsForGameType = clients.get(gameType);
            String client = clientsForGameType[random.nextInt(clientsForGameType.length)];

            Table table = createTemplate(id, gameType, template,  client, TableStatus.open, random.nextBoolean(), random.nextBoolean(), random.nextBoolean());
            tables.add(table);
        }
        System.out.println(String.format("Created %d tables", numberOfTables));
    }

    @Before
    public void setup() {
        tableSpace.clear(new Table());
        tableSpace.writeMultiple(tables.toArray());
    }

    @Test
    public void timeSQL() {
        SQLQuery<Table> query = new SQLQuery<Table>(Table.class, "partnerId=? and getGameType=? and clientId=? and templateName=? and tableStatus=? and full=? and showInLobby=? and availableForPlayersJoining=?");
        long duration = 0;
        for (int i = 0; i < totalRequests; i++) {
            long start = System.currentTimeMillis();

            query.setParameter(1, partnerId);
            query.setParameter(2, gameType);
            query.setParameter(3, clientId);
            query.setParameter(4, templateName);
            query.setParameter(5, tableStatus);
            query.setParameter(6, full);
            query.setParameter(7, showInLobby);
            query.setParameter(8, availableForPlayersJoining);

            Table[] tables = tableSpace.readMultiple(query, Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);

            long end = System.currentTimeMillis();
            duration += end - start;
        }

        double average = (double) duration / (double) totalRequests;

        System.out.println(String.format("Each SQL query took %fms", average));

    }

    @Test
    public void timeTemplating() {
        long duration = 0;
        for (int i = 0; i < totalRequests; i++) {

            long start = System.currentTimeMillis();

            final Table template = new Table();
            template.setGameTypeId(gameType);
            template.setClientId(clientId);
            template.setTemplateName(templateName);

            template.setTableStatus(TableStatus.open);
            template.setFull(false);
            template.setShowInLobby(true);
            template.setAvailableForPlayersJoining(true);

            final Table[] tables = tableSpace.readMultiple(template, Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);

            long end = System.currentTimeMillis();
            duration += end - start;

        }
        double average = (double) duration / (double) totalRequests;

        System.out.println(String.format("Each TEMPLATE query took %fms", average));

    }

    private static Table createTemplate(int id, String gameType, String templateName, String clientId, TableStatus tableStatus, boolean full, boolean showInLobby, boolean availableForJoining) {
        Table table = new Table();
        table.setTemplateName(templateName);
        table.setGameTypeId(gameType);
        table.setClientId(clientId);
        table.setTableStatus(tableStatus);
        table.setTableId(BigDecimal.valueOf(id));
        table.setFull(full);
        table.setShowInLobby(showInLobby);
        table.setAvailableForPlayersJoining(availableForJoining);
        return table;
    }

    private static Map<String, String[]> constructTemplateNames() {
        Map<String, String[]> names = new HashMap<String, String[]>();
        names.put("BLACKJACK", new String[]{"A more traditional form of blackjack *", "A more traditional form of blackjack **", "A more traditional form of blackjack ***", "Atlantic City", "Atlantic City Fast High", "Atlantic City Fast Low", "Atlantic City Fast Medium", "Atlantic City High", "Atlantic City Low", "Atlantic City Medium", "Blackjack daily tournament", "European Blackjack", "Get a blackjack and triple your money! *", "Get a blackjack and triple your money! **", "Get a blackjack and triple your money! ***", "Harder to card count but more favourable rules *", "Harder to card count but more favourable rules **", "Harder to card count but more favourable rules ***", "Have the advantage and see both the dealer's cards! *", "Have the advantage and see both the dealer's cards! **", "Have the advantage and see both the dealer's cards! ***", "High Roller", "Las Vegas Fast High", "Las Vegas Fast Low", "Las Vegas Fast Medium", "Las Vegas High", "Las Vegas Low", "Las Vegas Medium", "Super Blackjack 3:1"});
        names.put("SLOTS", new String[]{"Slots High", "Slots Low", "Slots Medium"});
        names.put("TEXAS_HOLDEM", new String[]{"Texas Holdem Fast High", "Texas Holdem Fast Low", "Texas Holdem Fast Medium", "Texas Holdem High", "Texas Holdem Low", "Texas Holdem Medium"});
        names.put("ROULETTE", new String[]{"Roulette Fast High", "Roulette Fast Low", "Roulette Fast Medium", "Roulette High", "Roulette Low", "Roulette Medium"});
        names.put("HISSTERIA", new String[]{"HISSTERIA default", "Hissteria High", "Hissteria Low", "Hissteria Medium"});

        return names;
    }

    private static Map<String, String[]> constructClients() {
        Map<String, String[]> names = new HashMap<String, String[]>();
        names.put("BLACKJACK", new String[]{"Blue Blackjack", "Blue Blackjack for Tournaments", "Red Blackjack", "Red Blackjack For Tournaments"});
        names.put("SLOTS", new String[]{"Default Slots"});
        names.put("TEXAS_HOLDEM", new String[]{"Default Texas Holdem Poker"});
        names.put("ROULETTE", new String[]{"Default Roulette"});
        names.put("HISSTERIA", new String[]{"HISSTERIA default"});
        return names;
    }


}
