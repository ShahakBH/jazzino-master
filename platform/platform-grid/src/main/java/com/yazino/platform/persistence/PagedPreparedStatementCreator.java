package com.yazino.platform.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PagedPreparedStatementCreator {
    /**
     * Create a prepared statement to retrieve a subset of the results.
     *
     * @param con         the connection.
     * @param offset      the offset i.e. the first result to fetch.
     * @param recordCount the number of records to fetch.
     * @return the prepared statement to retrieve this subset.
     * @throws SQLException if the statement preparation fails.
     */
    PreparedStatement createPreparedStatement(final Connection con, final long offset, final long recordCount)
            throws SQLException;
}
