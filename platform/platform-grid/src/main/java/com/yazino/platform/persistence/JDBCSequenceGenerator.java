package com.yazino.platform.persistence;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class JDBCSequenceGenerator implements SequenceGenerator {
    private final JdbcTemplate jdbcTemplate;
    private final int batchSize;

    private final List<Long> batchedKeys = new LinkedList<>();

    @Autowired
    public JDBCSequenceGenerator(@Qualifier("jdbcTemplate") final JdbcTemplate jdbcTemplate,
                                 final int batchSize) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");

        this.jdbcTemplate = jdbcTemplate;
        this.batchSize = batchSize;
    }

    @Transactional
    public synchronized BigDecimal next() {
        if (batchedKeys.isEmpty()) {
            retrieveBatch();
        }

        return BigDecimal.valueOf(batchedKeys.remove(0));
    }

    @Transactional
    public synchronized Set<BigDecimal> next(final int numberOfKeys) {
        if (numberOfKeys < 1) {
            throw new IllegalArgumentException("At least one key must be requested (asked for " + numberOfKeys + ")");
        }

        final Set<BigDecimal> keys = new HashSet<>();

        while (keys.size() < numberOfKeys) {
            if (batchedKeys.isEmpty()) {
                retrieveBatch();
            }
            keys.add(BigDecimal.valueOf(batchedKeys.remove(0)));
        }

        return keys;
    }

    private void retrieveBatch() {
        final Set<Long> batchOfKeys = jdbcTemplate.execute(
                new BatchStatementCreator(), new PrimaryKeyLoader());

        if (batchOfKeys == null || batchOfKeys.isEmpty()) {
            throw new IllegalStateException("Unable to fetch batch");
        }

        batchedKeys.addAll(batchOfKeys);
    }

    private class PrimaryKeyLoader implements PreparedStatementCallback<Set<Long>> {
        @Override
        public Set<Long> doInPreparedStatement(final PreparedStatement statement)
                throws SQLException, DataAccessException {
            statement.execute();

            final Set<Long> generatedKeys = new HashSet<Long>();

            ResultSet rs = null;
            try {
                rs = statement.getGeneratedKeys();
                while (rs.next()) {
                    generatedKeys.add(rs.getLong(1));
                }
                return generatedKeys;

            } finally {
                if (rs != null) {
                    try {
                        rs.close();

                    } catch (SQLException e) {
                        // ignored
                    }
                }
            }
        }
    }

    private class BatchStatementCreator implements PreparedStatementCreator {
        public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
            final Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
            final PreparedStatement ps = conn.prepareStatement(insertStatement(), Statement.RETURN_GENERATED_KEYS);
            for (int i = 1; i <= batchSize; ++i) {
                ps.setTimestamp(i, currentTimestamp);
            }
            return ps;
        }

        private String insertStatement() {
            final StringBuilder sqlStatement = new StringBuilder("INSERT INTO $SEQUENCE (TSALLOCATED) VALUES ");

            for (int i = 0; i < batchSize; ++i) {
                if (i > 0) {
                    sqlStatement.append(",");
                }
                sqlStatement.append("(?)");
            }

            return sqlStatement.toString();
        }
    }
}
