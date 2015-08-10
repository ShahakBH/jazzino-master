package com.yazino.platform.persistence.community;


import com.yazino.platform.model.community.PlayerTrophy;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import static com.yazino.platform.persistence.community.JDBCPlayerTrophyDAO.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Tests the {@link com.yazino.platform.persistence.community.JDBCPlayerTrophyDAO} class.
 */
public class JDBCPlayerTrophyDAOTest {

    private static final long D_06_05_2010_0900_BST = 1273132800000L;

    private final JdbcTemplate template = mock(JdbcTemplate.class);
    private final JDBCPlayerTrophyDAO dao = new JDBCPlayerTrophyDAO(template);
    private final ResultSet resultSet = mock(ResultSet.class);
    private final PreparedStatement preparedStatement = mock(PreparedStatement.class);

    @Test
    public void ensureRowMapperCreatesPlayerTrophy() throws Exception {
        RowMapper<PlayerTrophy> mapper = new PlayerTrophyRowMapper();
        when(resultSet.getBigDecimal(PLAYER_ID_COLUMN)).thenReturn(BigDecimal.valueOf(1));
        when(resultSet.getBigDecimal(TROPHY_ID_COLUMN)).thenReturn(BigDecimal.valueOf(1));
        when(resultSet.getTimestamp(DATE_AWARDED_COLUMN)).thenReturn(new Timestamp(D_06_05_2010_0900_BST));
        assertEquals(PlayerTrophy.class, mapper.mapRow(resultSet, 1).getClass());
    }

    @Test
    public void ensureRowMapperPopulatesPlayerTrophyWithCorrectData() throws Exception {
        RowMapper<PlayerTrophy> mapper = new PlayerTrophyRowMapper();
        when(resultSet.getBigDecimal(PLAYER_ID_COLUMN)).thenReturn(BigDecimal.valueOf(128));
        when(resultSet.getBigDecimal(TROPHY_ID_COLUMN)).thenReturn(BigDecimal.valueOf(56));
        when(resultSet.getTimestamp(DATE_AWARDED_COLUMN)).thenReturn(new Timestamp(D_06_05_2010_0900_BST));
        PlayerTrophy playerTrophy = mapper.mapRow(resultSet, 1);
        assertEquals(128, playerTrophy.getPlayerId().intValue());
        assertEquals(56, playerTrophy.getTrophyId().intValue());
        assertEquals(D_06_05_2010_0900_BST, playerTrophy.getAwardTime().getMillis());
    }

    @Test(expected = NullPointerException.class)
    public void ensureExceptionThrownWhenAttemptingToInsertNullPlayerTrophy() throws Exception {
        new JDBCPlayerTrophyDAO.PlayerTrophyPSSetter(null);
    }

    @Test
    public void ensureInsertExecutesCorrectInsertStatement() throws Exception {
        dao.insert(createPlayerTrophy(BigDecimal.valueOf(1), BigDecimal.valueOf(2), new DateTime()));
        verify(template).update(eq(DEFAULT_INSERT_QUERY), any(PreparedStatementSetter.class));
    }

    @Test
    public void ensurePlayerIdIsSetOnPreparedStatementCorrectly() throws Exception {
        BigDecimal playerId = BigDecimal.valueOf(5);
        PlayerTrophy playerTrophy = createPlayerTrophy(playerId, BigDecimal.valueOf(7), new DateTime(D_06_05_2010_0900_BST));
        JDBCPlayerTrophyDAO.PlayerTrophyPSSetter psSetter = new JDBCPlayerTrophyDAO.PlayerTrophyPSSetter(playerTrophy);
        psSetter.setValues(preparedStatement);
        verify(preparedStatement).setBigDecimal(1, playerId);
    }

    @Test
    public void ensureTrophyIdIsSetOnPreparedStatementCorrectly() throws Exception {
        BigDecimal trophyId = BigDecimal.valueOf(7);
        PlayerTrophy playerTrophy = createPlayerTrophy(BigDecimal.valueOf(5), trophyId, new DateTime(D_06_05_2010_0900_BST));
        JDBCPlayerTrophyDAO.PlayerTrophyPSSetter psSetter = new JDBCPlayerTrophyDAO.PlayerTrophyPSSetter(playerTrophy);
        psSetter.setValues(preparedStatement);
        verify(preparedStatement).setBigDecimal(2, trophyId);
    }

    @Test
    public void ensureAwardDateIsSetOnPreparedStatementCorrectly() throws Exception {
        DateTime awardTime = new DateTime(D_06_05_2010_0900_BST);
        PlayerTrophy playerTrophy = createPlayerTrophy(BigDecimal.valueOf(5), BigDecimal.valueOf(7), awardTime);
        JDBCPlayerTrophyDAO.PlayerTrophyPSSetter psSetter = new JDBCPlayerTrophyDAO.PlayerTrophyPSSetter(playerTrophy);
        psSetter.setValues(preparedStatement);
        // don't like this, not actually checking value of time
        verify(preparedStatement).setTimestamp(eq(3), any(Timestamp.class));
    }

    private static PlayerTrophy createPlayerTrophy(BigDecimal playerId, BigDecimal trophyId, DateTime awardTime) {
        return new PlayerTrophy(playerId, trophyId, awardTime);
    }

}
