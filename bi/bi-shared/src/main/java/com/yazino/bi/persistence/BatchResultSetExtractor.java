package com.yazino.bi.persistence;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class BatchResultSetExtractor<T> implements ResultSetExtractor<Integer> {
    private final RowMapper<T> rowMapper;
    private final BatchVisitor<T> visitor;
    private final int batchSize;

    public BatchResultSetExtractor(final RowMapper<T> rowMapper,
                                   final BatchVisitor<T> visitor,
                                   final int batchSize) {
        notNull(rowMapper, "rowMapper may not be null");
        notNull(visitor, "visitor may not be null");

        this.rowMapper = rowMapper;
        this.visitor = visitor;
        this.batchSize = batchSize;
    }

    @Override
    public Integer extractData(final ResultSet rs) throws SQLException, DataAccessException {
        final List<T> batch = new ArrayList<>(batchSize);
        int rowNumber = 0;
        while (rs.next()) {
            batch.add(rowMapper.mapRow(rs, rowNumber));

            if (batch.size() >= batchSize) {
                visitor.processBatch(batch);
                batch.clear();
            }

            ++rowNumber;
        }

        if (!batch.isEmpty()) {
            visitor.processBatch(batch);
        }

        return rowNumber;
    }
}
