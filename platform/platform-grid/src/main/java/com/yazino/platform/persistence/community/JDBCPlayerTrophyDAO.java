package com.yazino.platform.persistence.community;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.community.PlayerTrophy;
import com.yazino.platform.persistence.DataIterable;
import com.yazino.platform.persistence.ResultSetIterator;
import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Provides a JDBC implementation of the {@link PlayerTrophyDAO}.
 */
@Repository("playerTrophyDao")
public class JDBCPlayerTrophyDAO implements PlayerTrophyDAO, DataIterable<PlayerTrophy> {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCPlayerTrophyDAO.class);

    static final String PLAYER_ID_COLUMN = "PLAYER_ID";
    static final String TROPHY_ID_COLUMN = "TROPHY_ID";
    static final String DATE_AWARDED_COLUMN = "DATE_AWARDED";
    static final String SELECT_QUERY = String.format("SELECT %s,%s,%s FROM PLAYER_TROPHY WHERE %s > (now() - interval 53 week)",
            PLAYER_ID_COLUMN, TROPHY_ID_COLUMN, DATE_AWARDED_COLUMN, DATE_AWARDED_COLUMN);
    static final String DEFAULT_INSERT_QUERY = String.format("INSERT INTO PLAYER_TROPHY (%s,%s,%s)"
            + " VALUES (?,?,?) ON DUPLICATE KEY UPDATE %s=%s",
            PLAYER_ID_COLUMN, TROPHY_ID_COLUMN, DATE_AWARDED_COLUMN, PLAYER_ID_COLUMN, PLAYER_ID_COLUMN);

    private final PlayerTrophyRowMapper rowMapper = new PlayerTrophyRowMapper();
    private final JdbcTemplate template;

    @Autowired
    public JDBCPlayerTrophyDAO(final JdbcTemplate template) {
        notNull(template, "template must not be null");
        this.template = template;
    }

    @Override
    public DataIterator<PlayerTrophy> iterateAll() {
        return new ResultSetIterator<>(template, new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                return con.prepareStatement(SELECT_QUERY);
            }
        }, rowMapper);
    }

    @Override
    public void insert(final PlayerTrophy playerTrophy) {
        LOG.debug("Inserting PlayerTrophy {}", playerTrophy);
        template.update(DEFAULT_INSERT_QUERY, new PlayerTrophyPSSetter(playerTrophy));
    }

    static final class PlayerTrophyRowMapper implements RowMapper<PlayerTrophy> {

        @Override
        public PlayerTrophy mapRow(final ResultSet rs,
                                   final int rowNum) throws SQLException {
            final BigDecimal playerId = BigDecimals.strip(rs.getBigDecimal(PLAYER_ID_COLUMN));
            final BigDecimal trophyId = rs.getBigDecimal(TROPHY_ID_COLUMN);
            final Timestamp timestamp = rs.getTimestamp(DATE_AWARDED_COLUMN);

            final PlayerTrophy playerTrophy = new PlayerTrophy();
            playerTrophy.setPlayerId(playerId);
            playerTrophy.setTrophyId(trophyId);
            playerTrophy.setAwardTime(new DateTime(timestamp.getTime()));
            return playerTrophy;
        }
    }

    static final class PlayerTrophyPSSetter implements PreparedStatementSetter {

        private final PlayerTrophy playerTrophy;

        PlayerTrophyPSSetter(final PlayerTrophy playerTrophy) {
            notNull(playerTrophy, "playerTrophy must not be null");
            this.playerTrophy = playerTrophy;
        }

        @Override
        public void setValues(final PreparedStatement ps) throws SQLException {
            int index = 1;
            ps.setBigDecimal(index++, playerTrophy.getPlayerId());
            ps.setBigDecimal(index++, playerTrophy.getTrophyId());
            ps.setTimestamp(index, new Timestamp(playerTrophy.getAwardTime().getMillis()));
        }
    }


}
