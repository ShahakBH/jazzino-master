package com.yazino.platform.persistence.community;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.community.SystemMessage;
import com.yazino.platform.persistence.DataIterable;
import com.yazino.platform.persistence.ResultSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("systemMessageDao")
public class JDBCSystemMessageDAO implements SystemMessageDAO, DataIterable<SystemMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCSystemMessageDAO.class);

    private static final String SELECT_ALL = "SELECT * FROM SYSTEM_MESSAGE "
            + "WHERE VALID_TO >= CURRENT_DATE() ORDER BY VALID_FROM DESC";

    private final SystemMessageRowMapper systemMessageRowMapper = new SystemMessageRowMapper();

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JDBCSystemMessageDAO(final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "JDBC Template may not be null");

        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<SystemMessage> findValid() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding all valid system messages");
        }

        final Collection<SystemMessage> systemMessages = jdbcTemplate.query(SELECT_ALL, systemMessageRowMapper);
        if (systemMessages == null) {
            return Collections.emptyList();
        }

        return systemMessages;
    }

    @Override
    public DataIterator<SystemMessage> iterateAll() {
        return new ResultSetIterator<SystemMessage>(jdbcTemplate, new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                return con.prepareStatement(SELECT_ALL);
            }
        }, systemMessageRowMapper);
    }

    private class SystemMessageRowMapper implements RowMapper<SystemMessage> {
        @Override
        public SystemMessage mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final BigDecimal id = rs.getBigDecimal("SYSTEM_MESSAGE_ID");
            final String message = rs.getString("MESSAGE");
            final Date validFrom = getDate(rs, "VALID_FROM");
            final Date validTo = getDate(rs, "VALID_TO");

            return new SystemMessage(id, message, validFrom, validTo);
        }

        private Date getDate(final ResultSet rs, final String columnName) throws SQLException {
            final Timestamp timestamp = rs.getTimestamp(columnName);
            if (timestamp != null) {
                return new Date(timestamp.getTime());
            }
            return null;
        }
    }
}
