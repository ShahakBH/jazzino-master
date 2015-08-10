package com.yazino.platform.processor.table;

import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.PlayerInformationCache;
import com.yazino.platform.table.TableStatus;

import java.math.BigDecimal;
import java.util.*;

import static java.util.Arrays.asList;

/**
 * Generated tables with random properties.
 */
public class RandomTableGenerator {

    private final Random random = new Random();

    private String[] gameTypes = defaultGameTypes();

    private Map<String, String[]> templateNames = defaultTemplateNames();
    private Map<String, String[]> clients = defaultClients();
    private Set<String> tags = new HashSet<String>();

    private TableStatus[] statii = defaultTableStatii();

    private Boolean[] fulls = defaultBooleans();
    private Boolean[] showInLobbys = defaultBooleans();
    private Boolean[] availableForJoinings = defaultBooleans();

    public Set<Table> generateTables(int numberOfTables) {
        Set<Table> tables = new HashSet<Table>(numberOfTables);
        for (int i = 0; i < numberOfTables; i++) {
            int id = random.nextInt();
            String gameType = getRandomElement(gameTypes);
            String[] templatesForGameType = templateNames.get(gameType);
            String template = getRandomElement(templatesForGameType);

            String[] clientsForGameType = clients.get(gameType);
            String client = getRandomElement(clientsForGameType);

            TableStatus status = getRandomElement(statii);
            boolean full = getRandomElement(fulls);
            boolean showInLobby = getRandomElement(showInLobbys);
            boolean availableForJoining = getRandomElement(availableForJoinings);
            Set<String> tagsForTable = getRandomElements(tags);

            Table table = createTemplate(id, gameType, template, client, status, full, showInLobby, availableForJoining, tagsForTable);
            tables.add(table);
        }
        return tables;
    }

    private Set<String> getRandomElements(final Set<String> tags) {
        final List<String> sourceTags = new LinkedList<String>(tags);
        final Set<String> randomElements = new HashSet<String>();
        final int elementsToGet = random.nextInt(tags.size() + 1);
        for (int i = 0; i < elementsToGet; ++i) {
            randomElements.add(sourceTags.remove(0));
        }
        return randomElements;
    }


    public RandomTableGenerator setGameTypes(String... gameTypes) {
        this.gameTypes = gameTypes;
        return this;
    }

    public RandomTableGenerator setTemplateNames(Map<String, String[]> templateNames) {
        this.templateNames = templateNames;
        return this;
    }

    public RandomTableGenerator setClients(Map<String, String[]> clients) {
        this.clients = clients;
        return this;
    }

    public RandomTableGenerator setStatii(TableStatus... statii) {
        this.statii = statii;
        return this;
    }

    public RandomTableGenerator setFulls(Boolean... fulls) {
        this.fulls = fulls;
        return this;
    }

    public RandomTableGenerator setShowInLobbys(Boolean... showInLobbys) {
        this.showInLobbys = showInLobbys;
        return this;
    }

    public RandomTableGenerator setAvailableForJoinings(Boolean... availableForJoinings) {
        this.availableForJoinings = availableForJoinings;
        return this;
    }

    public RandomTableGenerator setTags(final String... tags) {
        this.tags = new HashSet<String>(asList(tags));
        return this;
    }

    private <T> T getRandomElement(T[] values) {
        return values[random.nextInt(values.length)];
    }

    private static Table createTemplate(int id, String gameType, String templateName, String clientId,
                                        TableStatus tableStatus, boolean full, boolean showInLobby, boolean availableForJoining,
                                        Set<String> tags) {
        Table table = new Table();
        table.setTemplateName(templateName);
        table.setGameTypeId(gameType);
        table.setClientId(clientId);
        table.setTableStatus(tableStatus);
        table.setTableId(BigDecimal.valueOf(id));
        table.setFull(full);
        table.setShowInLobby(showInLobby);
        table.setAvailableForPlayersJoining(availableForJoining);
        table.setPlayerInformationCache(new PlayerInformationCache());
        table.setTags(tags);
        return table;
    }

    private TableStatus[] defaultTableStatii() {
        return new TableStatus[]{TableStatus.closed, TableStatus.closing, TableStatus.error, TableStatus.open};
    }

    private Boolean[] defaultBooleans() {
        return new Boolean[]{true, false};
    }


    private static String[] defaultGameTypes() {
        return new String[]{"BLACKJACK", "SLOTS", "TEXAS_HOLDEM", "ROULETTE", "HISSTERIA"};
    }

    private static Map<String, String[]> defaultTemplateNames() {
        Map<String, String[]> names = new HashMap<String, String[]>();
        names.put("BLACKJACK", new String[]{"A more traditional form of blackjack *", "A more traditional form of blackjack **", "A more traditional form of blackjack ***", "Atlantic City", "Atlantic City Fast High", "Atlantic City Fast Low", "Atlantic City Fast Medium", "Atlantic City High", "Atlantic City Low", "Atlantic City Medium", "Blackjack daily tournament", "European Blackjack", "Get a blackjack and triple your money! *", "Get a blackjack and triple your money! **", "Get a blackjack and triple your money! ***", "Harder to card count but more favourable rules *", "Harder to card count but more favourable rules **", "Harder to card count but more favourable rules ***", "Have the advantage and see both the dealer's cards! *", "Have the advantage and see both the dealer's cards! **", "Have the advantage and see both the dealer's cards! ***", "High Roller", "Las Vegas Fast High", "Las Vegas Fast Low", "Las Vegas Fast Medium", "Las Vegas High", "Las Vegas Low", "Las Vegas Medium", "Super Blackjack 3:1"});
        names.put("SLOTS", new String[]{"Slots High", "Slots Low", "Slots Medium"});
        names.put("TEXAS_HOLDEM", new String[]{"Texas Holdem Fast High", "Texas Holdem Fast Low", "Texas Holdem Fast Medium", "Texas Holdem High", "Texas Holdem Low", "Texas Holdem Medium"});
        names.put("ROULETTE", new String[]{"Roulette Fast High", "Roulette Fast Low", "Roulette Fast Medium", "Roulette High", "Roulette Low", "Roulette Medium"});
        names.put("HISSTERIA", new String[]{"HISSTERIA default", "Hissteria High", "Hissteria Low", "Hissteria Medium"});

        return names;
    }

    private static Map<String, String[]> defaultClients() {
        Map<String, String[]> names = new HashMap<String, String[]>();
        names.put("BLACKJACK", new String[]{"Blue Blackjack", "Blue Blackjack for Tournaments", "Red Blackjack", "Red Blackjack For Tournaments"});
        names.put("SLOTS", new String[]{"Default Slots"});
        names.put("TEXAS_HOLDEM", new String[]{"Default Texas Holdem Poker"});
        names.put("ROULETTE", new String[]{"Default Roulette"});
        names.put("HISSTERIA", new String[]{"HISSTERIA default"});
        return names;
    }


}
