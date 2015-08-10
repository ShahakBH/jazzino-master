package com.yazino.platform.persistence.community;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.community.TableInvite;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCTableInviteDAOIntegrationTest {

    @Autowired
    public TableInviteDAO tableInviteDAO;
    @Autowired
    @Qualifier("jdbcTemplate")
    public JdbcTemplate jdbc;

    private static final long D_06_05_2010_0900_BST = 1273132800000L;

    @Before
    public void setUp() {
        jdbc.execute("DELETE FROM TABLE_INVITE");
    }

    @Test
    @Transactional
    public void saveShouldInsertTableInviteIntoDB() {
        BigDecimal playerId = new BigDecimal(123);
        BigDecimal tableId = new BigDecimal(321);
        DateTime inviteTime = new DateTime(D_06_05_2010_0900_BST);

        TableInvite invite = tableInvite(BigDecimal.valueOf(78), playerId, tableId, inviteTime, true);

        tableInviteDAO.save(invite);

        assertTableInviteInDBMatches(invite);
    }

    @Test
    @Transactional
    public void save_shouldUpdateExistingInvite() {
        BigDecimal playerId = new BigDecimal(123);
        BigDecimal tableId = new BigDecimal(321);
        DateTime inviteTime = new DateTime(D_06_05_2010_0900_BST);
        TableInvite invite = tableInvite(BigDecimal.valueOf(78), playerId, tableId, inviteTime, true);
        tableInviteDAO.save(invite);

        TableInvite expectedInvite = tableInvite(BigDecimal.valueOf(78), playerId, tableId, inviteTime, false);
        tableInviteDAO.save(expectedInvite);

        assertTableInviteInDBMatches(expectedInvite);
    }

    @Test
    @Transactional
    public void findAllShouldReturnAllOpenRows() {

        TableInvite test1 = tableInvite(BigDecimal.valueOf(34), new BigDecimal(1234), new BigDecimal(4321), new DateTime(D_06_05_2010_0900_BST), true);
        TableInvite test2 = tableInvite(BigDecimal.valueOf(897), new BigDecimal(4321), new BigDecimal(4321), new DateTime(D_06_05_2010_0900_BST), false);
        TableInvite test3 = tableInvite(BigDecimal.valueOf(666), new BigDecimal(777), new BigDecimal(888), new DateTime(D_06_05_2010_0900_BST), true);

        tableInviteDAO.save(test1);
        tableInviteDAO.save(test2);
        tableInviteDAO.save(test3);

        List<TableInvite> expectedInvites = Arrays.asList(test1, test3);

        final DataIterator<TableInvite> dataIterator = ((JDBCTableInviteDAO) tableInviteDAO).iterateAll();

        final List<TableInvite> tableInvites = new ArrayList<>();
        while (dataIterator.hasNext()) {
            tableInvites.add(dataIterator.next());
        }
        assertEquals(2, tableInvites.size());
        assertEquals(expectedInvites, tableInvites);

    }

    private void assertTableInviteInDBMatches(TableInvite invite) {
        SqlRowSet rowSet = jdbc.queryForRowSet("SELECT ID, PLAYER_ID, TABLE_ID, INVITE_TIME, OPEN FROM TABLE_INVITE WHERE ID = ? AND PLAYER_ID = ? AND TABLE_ID = ? AND INVITE_TIME = ? AND OPEN = ?",
                invite.getId(), invite.getPlayerId(), invite.getTableId(), invite.getInviteTime().toDate(), invite.isOpen());

        if (rowSet.next()) {
            assertEquals(invite.getId(), rowSet.getBigDecimal("ID"));
            assertThat(rowSet.getBigDecimal("PLAYER_ID"), is(comparesEqualTo(invite.getPlayerId())));
            assertThat(rowSet.getBigDecimal("TABLE_ID"), is(comparesEqualTo(invite.getTableId())));
            assertEquals(new Timestamp(invite.getInviteTime().getMillis()), rowSet.getTimestamp("INVITE_TIME"));
            assertTrue(invite.isOpen() == rowSet.getBoolean("OPEN"));
        } else {
            fail("There should be a row that matches the tableInvite");
        }
    }

    private TableInvite tableInvite(BigDecimal id, BigDecimal playerId, BigDecimal tableId, DateTime inviteTime, Boolean open) {
        TableInvite invite = new TableInvite(playerId, tableId, inviteTime, open);
        invite.setId(id);
        return invite;
    }
}
