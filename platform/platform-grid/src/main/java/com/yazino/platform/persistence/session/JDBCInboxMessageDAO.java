package com.yazino.platform.persistence.session;

import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.yazino.game.api.NewsEvent;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class JDBCInboxMessageDAO implements InboxMessageDAO {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCInboxMessageDAO.class);

    private static final String SELECT_FOR_PLAYER
            = "SELECT * FROM PLAYER_INBOX WHERE PLAYER_ID=? AND IS_READ=? ORDER BY RECEIVED_TIME ASC";
    private static final String INSERT_OR_UPDATE_TROPHY = "INSERT INTO PLAYER_INBOX "
            + "(PLAYER_ID,RECEIVED_TIME,IS_READ,MESSAGE) "
            + "VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE IS_READ=VALUES(IS_READ)";

    private final InboxMessageRowMapper trophyRowMapper = new InboxMessageRowMapper();
    private final NewsEventSerializer newsSerializer = new NewsEventSerializer();

    private JdbcTemplate jdbcTemplate;


    @Autowired
    public JDBCInboxMessageDAO(@Qualifier("jdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "JDBC Template may not be null");
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(final InboxMessage message) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering save " + ReflectionToStringBuilder.reflectionToString(message));
        }

        jdbcTemplate.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(INSERT_OR_UPDATE_TROPHY);

                int index = 1;

                st.setBigDecimal(index++, message.getPlayerId());
                st.setTimestamp(index++, new Timestamp(message.getReceivedTime().getMillis()));
                st.setBoolean(index++, message.isRead());
                st.setString(index, newsSerializer.serialize(message.getNewsEvent()));
                return st;
            }
        });

        if (LOG.isDebugEnabled()) {
            LOG.debug("Saved " + ReflectionToStringBuilder.reflectionToString(message));
        }
    }


    @Override
    public List<InboxMessage> findUnreadMessages(final BigDecimal playerId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering findUnreadMessages " + playerId);
        }

        final List<InboxMessage> unreadMessages = jdbcTemplate.query(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(SELECT_FOR_PLAYER);
                int index = 1;
                st.setBigDecimal(index++, playerId);
                st.setBoolean(index, false);
                return st;
            }
        }, trophyRowMapper);
        if (unreadMessages == null) {
            return Collections.emptyList();
        }
        return unreadMessages;
    }

    private class InboxMessageRowMapper implements RowMapper<InboxMessage> {
        @Override
        public InboxMessage mapRow(final ResultSet resultSet,
                                   final int row) throws SQLException {
            final BigDecimal playerId = BigDecimals.strip(resultSet.getBigDecimal("PLAYER_ID"));
            final NewsEvent newsEvent = deserializeNewsEvent(resultSet.getString("MESSAGE"));
            final DateTime receivedTime = new DateTime(resultSet.getTimestamp("RECEIVED_TIME").getTime());
            final InboxMessage result = new InboxMessage(playerId, newsEvent, receivedTime);
            result.setRead(resultSet.getBoolean("IS_READ"));
            return result;
        }

        private NewsEvent deserializeNewsEvent(final String text) throws SQLException {
            try {
                final NewsEvent deserialisedNews = newsSerializer.deserialize(text);
                if (deserialisedNews == null) {
                    throw new IllegalArgumentException("Could not deserialise '" + text + "'");
                }
                return deserialisedNews;

            } catch (RuntimeException e) {
                LOG.error("Couldn't deserialize news event '" + text + "'", e);
                throw new IllegalStateException("Couldn't deserialize news event '" + text + "'", e);
            }
        }
    }
}
