package com.yazino.platform.persistence.community;

import com.yazino.platform.community.Trophy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("trophyDao")
public class JDBCTrophyDAO implements TrophyDAO {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCTrophyDAO.class);

    private static final String SELECT_ALL = "SELECT * FROM TROPHY";
    private static final String INSERT_OR_UPDATE_TROPHY = "INSERT INTO TROPHY "
            + "(TROPHY_ID,TROPHY_IMAGE,TROPHY_NAME,GAME_TYPE, MESSAGE, SHORT_DESCRIPTION, MESSAGE_CABINET) "
            + "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE TROPHY_IMAGE=VALUES(TROPHY_IMAGE),"
            + "TROPHY_NAME=VALUES(TROPHY_NAME), MESSAGE=VALUES(MESSAGE),"
            + " SHORT_DESCRIPTION=VALUES(SHORT_DESCRIPTION), MESSAGE_CABINET=VALUES(MESSAGE_CABINET)";

    private final TrophyRowMapper trophyRowMapper = new TrophyRowMapper();

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public JDBCTrophyDAO(final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "JDBC Template may not be null");

        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<Trophy> retrieveAll() {
        LOG.debug("Entering findAll");

        final List<Trophy> trophies = jdbcTemplate.query(SELECT_ALL, trophyRowMapper);
        if (trophies == null) {
            return Collections.emptyList();
        }
        return trophies;
    }

    @Override
    public void save(final Trophy trophy) {
        notNull(trophy, "Trophy may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering save " + trophy);
        }

        jdbcTemplate.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(INSERT_OR_UPDATE_TROPHY);

                int index = 1;

                st.setBigDecimal(index++, trophy.getId());
                st.setString(index++, trophy.getImage());
                st.setString(index++, trophy.getName());
                st.setString(index++, trophy.getGameType());
                st.setString(index++, trophy.getMessage());
                st.setString(index++, trophy.getShortDescription());
                st.setString(index, trophy.getMessageCabinet());

                return st;
            }
        });

        if (LOG.isDebugEnabled()) {
            LOG.debug("Saved " + trophy);
        }
    }

    private class TrophyRowMapper implements RowMapper<Trophy> {
        @Override
        public Trophy mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final BigDecimal id = rs.getBigDecimal("TROPHY_ID");
            final String image = rs.getString("TROPHY_IMAGE");
            final String name = rs.getString("TROPHY_NAME");
            final String gameType = rs.getString("GAME_TYPE");
            final String message = rs.getString("MESSAGE");
            final String shortDescription = rs.getString("SHORT_DESCRIPTION");
            final String messageCabinet = rs.getString("MESSAGE_CABINET");
            final Trophy trophy = new Trophy(id, name, gameType, image);
            trophy.setMessage(message);
            trophy.setShortDescription(shortDescription);
            trophy.setMessageCabinet(messageCabinet);
            return trophy;
        }
    }
}
