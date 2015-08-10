package com.yazino.platform.persistence.community;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.model.community.Gift;
import com.yazino.platform.persistence.ResultSetIterator;
import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.Date;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.Validate.notNull;

@Repository("giftDao")
public class JDBCGiftDAO implements GiftDAO {
    private static final String PROPERTY_GIFT_RETENTION = "gifting.retention-hours";
    private static final int DEFAULT_GIFT_RETENTION = 168;
    private static final String SQL_SELECT_ALL = "SELECT * FROM GIFTS WHERE CREATED_TS > ?";
    private static final String SQL_UPSERT = "INSERT INTO GIFTS "
            + "(GIFT_ID, SENDER_ID, RECEIVER_ID, CREATED_TS, EXPIRY_TS, COLLECTED_TS, ACKNOWLEDGED) "
            + "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
            + "COLLECTED_TS=VALUES(COLLECTED_TS), ACKNOWLEDGED=VALUES(ACKNOWLEDGED)";

    private final GiftRowMapper rowMapper = new GiftRowMapper();

    private final YazinoConfiguration yazinoConfiguration;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JDBCGiftDAO(final JdbcTemplate jdbcTemplate,
                       final YazinoConfiguration yazinoConfiguration) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.jdbcTemplate = jdbcTemplate;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @Override
    public void save(final Gift gift) {
        notNull(gift, "gift may not be null");

        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                final PreparedStatement stmt = con.prepareStatement(SQL_UPSERT);

                int index = 1;
                stmt.setBigDecimal(index++, gift.getId());
                stmt.setBigDecimal(index++, gift.getSendingPlayer());
                stmt.setBigDecimal(index++, gift.getRecipientPlayer());
                stmt.setTimestamp(index++, timestamp(gift.getCreated()));
                stmt.setTimestamp(index++, timestamp(gift.getExpiry()));
                stmt.setTimestamp(index++, timestamp(gift.getCollected()));
                stmt.setBoolean(index, defaultIfNull(gift.getAcknowledged(), false));

                return stmt;
            }
        });
    }

    private Timestamp timestamp(final DateTime dateTime) {
        if (dateTime != null) {
            return new Timestamp(dateTime.getMillis());
        }
        return null;
    }

    @Override
    public DataIterator<Gift> iterateAll() {
        final int retentionInHours = yazinoConfiguration.getInt(PROPERTY_GIFT_RETENTION, DEFAULT_GIFT_RETENTION);
        return new ResultSetIterator<>(jdbcTemplate, new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                final PreparedStatement stmt = con.prepareStatement(SQL_SELECT_ALL);
                stmt.setTimestamp(1, timestamp(new DateTime().minusHours(retentionInHours)));
                return stmt;
            }
        }, rowMapper);
    }

    private class GiftRowMapper implements RowMapper<Gift> {
        @Override
        public Gift mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new Gift(BigDecimals.strip(rs.getBigDecimal("gift_id")),
                    BigDecimals.strip(rs.getBigDecimal("sender_id")),
                    BigDecimals.strip(rs.getBigDecimal("receiver_id")),
                    dateTime(rs.getTimestamp("created_ts")),
                    dateTime(rs.getTimestamp("expiry_ts")),
                    dateTime(rs.getTimestamp("collected_ts")),
                    defaultIfNull(rs.getBoolean("acknowledged"), false));
        }

        private DateTime dateTime(final Date date) {
            if (date != null) {
                return new DateTime(date);
            }
            return null;
        }
    }
}
