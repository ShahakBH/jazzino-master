package com.yazino.platform.persistence;

import com.gigaspaces.datasource.DataIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.apache.commons.lang3.Validate.notNull;

public class ResultSetIterator<T> implements DataIterator<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ResultSetIterator.class);

    private final RowMapper rowMapper;

    private DataSource dataSource;
    private ResultSet resultSet;
    private Connection connection;

    private int rowNumber;

    public ResultSetIterator(final JdbcTemplate template,
                             final PreparedStatementCreator preparedStatementCreator,
                             final RowMapper<T> rowMapper) {
        notNull(template, "Template may not be null");
        notNull(preparedStatementCreator, "Prepared Statement Creator may not be null");
        notNull(rowMapper, "Row Mapper may not be null");

        this.rowMapper = rowMapper;

        PreparedStatement stmt = null;
        ResultSet currentResultSet = null;

        try {
            dataSource = template.getDataSource();
            connection = DataSourceUtils.getConnection(dataSource);

            stmt = preparedStatementCreator.createPreparedStatement(connection);
            currentResultSet = stmt.executeQuery();

        } catch (SQLException e) {
            LOG.error("Could not load iterator", e);

            JdbcUtils.closeResultSet(currentResultSet);
            JdbcUtils.closeStatement(stmt);

            DataSourceUtils.releaseConnection(connection, dataSource);

            String sql = null;
            if (stmt != null) {
                sql = stmt.toString();
            }
            throw new UncategorizedSQLException("iterator", sql, e);
        }
        this.resultSet = currentResultSet;
    }

    public boolean hasNext() {
        try {
            if (resultSet.isLast() || resultSet.isAfterLast()) {
                return false;
            }

            // if no rows have been consumed and isBeforeFirst returns false then we have no results
            return !(rowNumber == 0 && !resultSet.isBeforeFirst());

        } catch (SQLException e) {
            LOG.error("Could not query result set for next", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public T next() {
        try {
            // we manually track row number as the result set support is *not* required by the spec
            ++rowNumber;
            resultSet.next();

            return (T) rowMapper.mapRow(resultSet, rowNumber);

        } catch (Exception e) {
            LOG.error("Could not iterate to next result", e);
            return null;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("Remove support is not available");
    }

    public void close() {
        JdbcUtils.closeResultSet(resultSet);

        DataSourceUtils.releaseConnection(connection, dataSource);
    }

}
