package com.yazino.platform.persistence.table;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.GameType;
import com.yazino.game.api.PlayerAtTableInformation;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.persistence.PrimaryKeyLoader;
import com.yazino.platform.repository.table.GameConfigurationRepository;
import com.yazino.platform.repository.table.GameVariationRepository;
import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.test.PrintlnRules;
import com.yazino.platform.test.PrintlnStatus;
import com.yazino.platform.util.Visitor;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCTableDAOIntegrationTest {
    private static final String GAME_TYPE = "PRINTLN";
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(-20000);

    @Autowired
    private TableDAO tableDAO;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private GameVariationRepository gameTemplateRepository;

    @Autowired
    private GameConfigurationRepository gameConfigurationRepository;

    private BigDecimal tableId;
    private BigDecimal accountId3;
    private BigDecimal accountId4;
    private int numtables; // global so that we can use it in an inner class
    private BigDecimal gameVariationTemplateId;

    @Before
    public void setUp() {

        jdbc.update("DELETE FROM ACCOUNT WHERE ACCOUNT_ID < 0");

        jdbc.update("INSERT INTO ACCOUNT(ACCOUNT_ID,NAME) VALUES (?,'user test account 1')", BigDecimal.valueOf(-100));
        accountId3 = BigDecimal.valueOf(-300);
        jdbc.update("INSERT INTO ACCOUNT(ACCOUNT_ID,NAME) VALUES (?,'user test account 2')", accountId3);
        accountId4 = BigDecimal.valueOf(-400);
        jdbc.update("INSERT INTO ACCOUNT(ACCOUNT_ID,NAME) VALUES (?,'user test account 3')", accountId4);
        gameVariationTemplateId = jdbc.execute(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                return con.prepareStatement("INSERT INTO GAME_VARIATION_TEMPLATE(GAME_TYPE, NAME, VERSION) VALUES ('BLACKJACK','Test variation',0)", PreparedStatement.RETURN_GENERATED_KEYS);
            }
        }, new PrimaryKeyLoader());// ensure there is an entry in GAME_VARIATION_TEMPLATE
        jdbc.update(format("INSERT INTO GAME_VARIATION_TEMPLATE_PROPERTY(GAME_VARIATION_TEMPLATE_ID, NAME, VALUE, VERSION) VALUES (%s,'BET_TIMEOUTS_ALLOWED','2',0)", gameVariationTemplateId));

        gameTemplateRepository.refreshAll();

        jdbc.update(format("DELETE FROM GAME_CONFIGURATION WHERE GAME_ID = '%s'", GAME_TYPE));
        GameConfiguration gc = gameConfiguration();
        jdbc.update(format("INSERT INTO GAME_CONFIGURATION (GAME_ID,SHORT_NAME,DISPLAY_NAME,ALIASES,ORD) VALUES ('%s','%s','%s','%s',%s)",
                gc.getGameId(), gc.getShortName(), gc.getDisplayName(), "printLn,PRINT_LN,println,PrintLn,pl", gc.getOrder()));
        gameConfigurationRepository.refreshAll();
    }

    @Test
    public void testCreateTableInfo() {
        Table ti = setupTable(100L, TableStatus.open, true);
        Table t2 = tableDAO.findById(tableId);
        //dont know the created timestamp until saved in db
        ti.setCreatedDateTime(t2.getCreatedDateTime());

        ti.setCurrentGame(null);
        assertEquals(ti, t2);
    }

    @Test
    public void testCreateTableWithOwner() {
        Table t1 = setupTable(200L, TableStatus.open, new BigDecimal(123), true);
        Table t2 = tableDAO.findById(tableId);
        t1.setCurrentGame(null);
        assertEquals(t2, t1);
    }

    @Test
    public void testCreateTableWithTags() {
        Table t1 = setupTable(200L, TableStatus.open, true, "aTag", "anotherTag");

        Table t2 = tableDAO.findById(tableId);
        t1.setCurrentGame(null);
        assertEquals(t2, t1);
    }

    @Test
    public void testUpdateTableWithOwner() {
        Table t1 = setupTable(300L, TableStatus.open, new BigDecimal(123), true);
        t1.setOwnerId(new BigDecimal(321));
        tableDAO.save(t1);
        Table t2 = tableDAO.findById(tableId);
        t1.setCurrentGame(null);
        assertEquals(t2, t1);
    }

    @Test
    public void testUpdateTableWithTags() {
        Table t1 = setupTable(300L, TableStatus.open, true, "aTag", "anotherTag");

        t1.setTags(newHashSet("andAnotherTag", "aTag"));
        tableDAO.save(t1);

        Table t2 = tableDAO.findById(tableId);
        t1.setCurrentGame(null);
        assertEquals(t2, t1);
    }

    private int countTables() {
        numtables = 0;
        tableDAO.visitTables(TableStatus.open, new Visitor<Table>() {
            public void visit(Table object) {
                numtables++;
            }
        });
        return numtables;
    }

    private Table setupTable(long idBase, TableStatus status, boolean createStatus, String... tags) {
        return setupTable(idBase, status, null, createStatus, tags);
    }

    private Table setupTable(long idBase,
                             TableStatus status,
                             BigDecimal ownerId,
                             boolean createStatus,
                             String... tags) {
        Table table = new Table();
        if (createStatus) {
            PrintlnStatus ps1 = new PrintlnStatus();
            ps1.setId(idBase + 20);
            ps1.setName("Test1");
            Collection<PlayerAtTableInformation> playerAtTableInformationCollection = Arrays.asList(
                    new PlayerAtTableInformation(new GamePlayer(accountId3, null, ""), Collections.<String, String>emptyMap()),
                    new PlayerAtTableInformation(new GamePlayer(accountId4, null, ""), Collections.<String, String>emptyMap())
            );
            ps1.setPlayersAtTable(playerAtTableInformationCollection);
            table.setCurrentGame(new GameStatus(ps1));
        }
        table.setGameId(idBase + 2l);
        table.setGameType(new GameType(new PrintlnRules().getGameType(), "PrintLn", newHashSet("printLn", "PRINT_LN", "println", "PrintLn", "pl")));
        table.setTableName("test");
        table.setTableStatus(status);
        table.setShowInLobby(true);
        table.setTemplateId(gameVariationTemplateId);
        table.setClientId("BJ-1");
        table.setOwnerId(ownerId);
        if (tags != null) {
            table.setTags(newHashSet(tags));
        }

        tableId = BigDecimal.valueOf(idBase + 1024);
        table.setTableId(tableId);
        tableDAO.save(table);

        gameTemplateRepository.populateProperties(table);

        table.setTemplateName(jdbc.queryForObject(
                "SELECT name FROM GAME_VARIATION_TEMPLATE where game_variation_template_id=?",
                new Object[]{table.getTemplateId()}, String.class));
        table.setCreatedDateTime(new DateTime(jdbc.queryForObject(
                "SELECT tscreated FROM TABLE_INFO where table_id=?",
                new Object[]{table.getTableId()}, Date.class)));

        return table;
    }

    @Test
    public void test_visitTables() {
        int before = countTables();
        setupTable(-100L, TableStatus.open, true);
        setupTable(-200L, TableStatus.open, true);
        setupTable(-300L, TableStatus.open, true);
        setupTable(-400L, TableStatus.closing, true);
        setupTable(-500L, TableStatus.closed, true);
        assertEquals(before + 3, countTables());
    }

    @Test
    public void testTableIterator() {
        final HashSet<Table> expected = new HashSet<>();
        expected.add(setupTable(-100L, TableStatus.open, new BigDecimal(321), false));
        expected.add(setupTable(-200L, TableStatus.open, new BigDecimal(322), false));
        expected.add(setupTable(-300L, TableStatus.open, new BigDecimal(323), false));
        setupTable(-400L, TableStatus.closing, true);
        setupTable(-500L, TableStatus.closed, true);

        final DataIterator<Table> iterator = ((JDBCTableDAO) tableDAO).iterateAll();

        final HashSet<Table> results = new HashSet<>();
        while (iterator.hasNext()) {
            results.add(iterator.next());
        }
        iterator.close();

        assertEquals(expected, results);
    }

    @Test
    public void testSaveAndLoadTableInfoWithNullCurrentStatus() {
        Table table = setupTable(100L, TableStatus.open, true);
        table.setCurrentGame(null);
        table.setShowInLobby(true);
        tableDAO.save(table);
        Table fromDatabase = tableDAO.findById(table.getTableId());
        table.setCreatedDateTime(fromDatabase.getCreatedDateTime());
        assertEquals(table, fromDatabase);
    }

    @Test
    public void anUpdatesModifiesTheExistingRecordWithChanges() {
        final Table newTable = aTable();

        tableDAO.save(newTable);

        newTable.setTableStatus(TableStatus.closed);
        newTable.setGameTypeId("ROULETTE");
        newTable.setLastUpdated(100000000L);

        tableDAO.save(newTable);

        Map map = jdbc.queryForMap("SELECT count(TABLE_ID) AS count FROM TABLE_INFO WHERE TABLE_ID=?", TABLE_ID);
        assertEquals((long) 1, map.get("count"));

        map = jdbc.queryForMap("SELECT * FROM TABLE_INFO WHERE TABLE_ID=?", TABLE_ID);
        assertEquals(0, TABLE_ID.compareTo((BigDecimal) map.get("TABLE_ID")));
        assertEquals("ROULETTE", map.get("GAME_TYPE"));
        assertEquals(TableStatus.closed.getStatusName(), map.get("STATUS"));
        assertEquals(new Timestamp(100000000L), map.get("TS"));
    }

    @Test
    public void anInsertReturnsTrue() {
        assertThat(tableDAO.save(aTable()), is(true));
    }

    @Test
    public void anInsertReturnsFalse() {
        final Table table = aTable();
        tableDAO.save(table);
        table.setLastUpdated(1000000000L);

        assertThat(tableDAO.save(aTable()), is(false));
    }

    @Test
    public void updateDefaultsLastUpdatedTimestampIfNotSet() {
        final Table newTable = aTable();
        tableDAO.save(newTable);

        newTable.setGameTypeId("ROULETTE");

        // we subtract 1s here as the lack of MS in MySQL appears to round us down on occasion
        final long timestampLowerBound = currentTimestampWithoutMillis() - 1000;

        tableDAO.save(newTable);

        // and add 1s to catch round ups
        final long timestampUpperBound = currentTimestampWithoutMillis() + 1000;

        final Map<String, Object> record = jdbc.queryForMap("SELECT * FROM TABLE_INFO WHERE TABLE_ID=?", TABLE_ID);
        final long recordUpdatedTimestamp = ((Timestamp) record.get("TS")).getTime();
        assertTrue("Timestamp failed expectation: " + timestampLowerBound + " <= " + recordUpdatedTimestamp + " <= " + timestampUpperBound,
                recordUpdatedTimestamp >= timestampLowerBound && recordUpdatedTimestamp <= timestampUpperBound);
    }

    private long currentTimestampWithoutMillis() {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0); // MySQL scorns such things
        return cal.getTimeInMillis();
    }

    private Table aTable() {

        final Table newTable = new Table();
        newTable.setTableId(TABLE_ID);
        newTable.setCurrentGame(new GameStatus(gameStatus()));
        newTable.setGameId(3000L);
        newTable.setGameTypeId(GAME_TYPE);
        newTable.setTableName("test");
        newTable.setTableStatus(TableStatus.open);
        newTable.setTemplateId(gameVariationTemplateId);
        newTable.setShowInLobby(true);
        newTable.setVariationProperties(variationProperties());
        return newTable;
    }

    private PrintlnStatus gameStatus() {
        final PrintlnStatus ps1 = new PrintlnStatus();
        ps1.setId(2000L);
        ps1.setName("Test1");
        return ps1;
    }

    private Map<String, String> variationProperties() {
        final Map<String, String> variationProperties = new HashMap<>();
        variationProperties.put("TEST1", "VAL1");
        variationProperties.put("TEST2", "VAL2");
        variationProperties.put("TEST3", "VAL3");
        return variationProperties;
    }

    private GameConfiguration gameConfiguration() {
        return new GameConfiguration(GAME_TYPE, "println", "PrintLn", new HashSet<>(Arrays.asList("printLn", "PRINT_LN", "println", "PrintLn", "pl")), 0);
    }

}
