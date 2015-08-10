package com.yazino.platform.player.persistence;

import org.springframework.jdbc.core.PreparedStatementCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.apache.commons.lang3.Validate.notNull;

class SimpleQueryCreator implements PreparedStatementCreator {
    private final String query;
    private final Object[] parameters;

    public SimpleQueryCreator(final String query,
                              final Object... parameters) {
        this.query = query;
        this.parameters = parameters;
    }

    @Override
    public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
        notNull(conn, "Connection may not be null");

        final PreparedStatement stmt = conn.prepareStatement(query);

        if (parameters != null) {
            int index = 1;
            for (final Object parameter : parameters) {
                stmt.setObject(index++, parameter);
            }
        }

        return stmt;
    }
}
