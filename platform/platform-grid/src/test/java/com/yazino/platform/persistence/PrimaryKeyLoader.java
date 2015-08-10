package com.yazino.platform.persistence;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PrimaryKeyLoader implements PreparedStatementCallback<BigDecimal> {
    @Override
    public BigDecimal doInPreparedStatement(final PreparedStatement statement)
            throws SQLException, DataAccessException {
        statement.execute();

        ResultSet rs = null;
        try {
            rs = statement.getGeneratedKeys();
            if (rs.next()) {
                return rs.getBigDecimal(1);
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
