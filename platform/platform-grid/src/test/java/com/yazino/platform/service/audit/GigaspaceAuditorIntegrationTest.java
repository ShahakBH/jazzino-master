package com.yazino.platform.service.audit;

import com.yazino.game.api.Command;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.GameType;
import com.yazino.platform.model.table.ClosedGameAuditWrapper;
import com.yazino.platform.model.table.CommandAuditWrapper;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.test.PrintlnRules;
import com.yazino.platform.test.PrintlnStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "transactionManager")
public class GigaspaceAuditorIntegrationTest {
    private static final GameType GAME_TYPE = new GameType("PRINTLN", "PrintLn", Collections.<String>emptySet());

    @Autowired
    public GigaspaceAuditor auditor;
    @Autowired
    public GigaSpace gigaSpace;

    private BigDecimal tableId = BigDecimal.valueOf(100.2);
    private BigDecimal tableAccountId = BigDecimal.valueOf(50.1);
    private Long gameId = 3l;

    @Before
    public void clearSpace() {
        gigaSpace.clear(null);
    }

    @Test
    public void auditorIsEnabledByDefault() {
        final Command c = new Command(new GamePlayer(tableAccountId, null, "y"), tableId, gameId, "12345-6", "TYPE1");

        auditor.audit("12345", c);

        assertThat(gigaSpace.take(new CommandAuditWrapper()), is(not(nullValue())));
    }

    @Test
    @DirtiesContext
    public void auditorDoesNotAddItemsToSpaceWhenDisabled() {
        final Command c = new Command(new GamePlayer(tableAccountId, null, "y"), tableId, gameId, "12345-6", "TYPE1");

        auditor.setEnabled(false);
        auditor.audit("12345", c);

        assertThat(gigaSpace.take(new CommandAuditWrapper()), is(nullValue()));
    }

    @Test
    public void audit_command_inserts_an_audit_command_wrapper() {
        Command c = new Command(new GamePlayer(tableAccountId, null, "y"), tableId, gameId, "12345-6", "TYPE1");
        auditor.audit("12345", c);
        CommandAuditWrapper caw = gigaSpace.take(new CommandAuditWrapper());
        assertEquals("12345", caw.getAuditContext().getLabel());
        assertEquals(c, caw.getCommand());
    }

    @Test
    public void audit_table_inserts_a_closed_game_when_complete() {
        Table table = new Table(GAME_TYPE, null, null, true);
        table.setTableId(tableId);
        table.setVariationProperties(new HashMap<String, String>());
        table.setTableStatus(TableStatus.open);
        table.setCurrentGame(new GameStatus(new PrintlnStatus()));
        table.setGameId(1L);
        table.setTableStatus(TableStatus.open);
        auditor.audit("12345", table, new PrintlnRules());
        ClosedGameAuditWrapper gaw = gigaSpace.take(new ClosedGameAuditWrapper());
        assertEquals("12345", gaw.getAuditContext().getLabel());
    }

    @Test
    public void testHostNameNotNull() {
        assertNotNull(auditor.getHostname());
    }

}
