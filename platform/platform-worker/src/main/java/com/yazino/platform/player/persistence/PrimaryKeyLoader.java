package com.yazino.platform.player.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class PrimaryKeyLoader implements PreparedStatementCallback<Long> {
    private static final Logger LOG = LoggerFactory.getLogger(PrimaryKeyLoader.class);

    public Long doInPreparedStatement(final PreparedStatement statement)
            throws SQLException, DataAccessException {
        statement.execute();

        ResultSet rs = null;
        try {
            rs = statement.getGeneratedKeys();
            if (rs.next()) {
                final long key = rs.getLong(1);
                LOG.debug("readAggregate key " + key);
                return key;
            }
            return null;

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
