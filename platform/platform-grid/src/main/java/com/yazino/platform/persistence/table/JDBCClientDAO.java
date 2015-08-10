package com.yazino.platform.persistence.table;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.persistence.DataIterable;
import com.yazino.platform.persistence.ResultSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("clientDao")
public class JDBCClientDAO implements ClientDAO, DataIterable<Client> {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCClientDAO.class);

    private static final String SELECT_PROPERTIES
            = "SELECT PROPERTY_NAME,PROPERTY_VALUE FROM CLIENT_PROPERTY WHERE CLIENT_ID=?";
    private static final String SELECT_ALL = "SELECT * FROM CLIENT";

    private final ClientRowMapper rowMapper = new ClientRowMapper();
    private final ClientPropertyExtractor clientPropertyExtractor = new ClientPropertyExtractor();

    private final JdbcTemplate jdbcTemplate;

    @Autowired(required = true)
    public JDBCClientDAO(@Qualifier("jdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");

        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<Client> findAll() {
        LOG.debug("Finding all");

        return jdbcTemplate.query(SELECT_ALL, rowMapper);
    }

    @Override
    public DataIterator<Client> iterateAll() {
        return new ResultSetIterator<Client>(jdbcTemplate, new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                return con.prepareStatement(SELECT_ALL);
            }
        }, rowMapper);
    }

    private class ClientRowMapper implements RowMapper<Client> {
        public Client mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final String clientId = rs.getString("CLIENT_ID");
            final String clientFile = rs.getString("CLIENT_FILE");
            final String gameType = rs.getString("GAME_TYPE");
            final int numberOfSeats = rs.getInt("NUMBER_OF_SEATS");

            final Map<String, String> clientProperties = jdbcTemplate.query(
                    SELECT_PROPERTIES, clientPropertyExtractor, clientId);

            return new Client(clientId, numberOfSeats, clientFile, gameType, clientProperties);
        }
    }

    private class ClientPropertyExtractor implements ResultSetExtractor<Map<String, String>> {
        @Override
        public Map<String, String> extractData(final ResultSet rs)
                throws SQLException, DataAccessException {
            final Map<String, String> clientProperties = new HashMap<String, String>();

            while (rs.next()) {
                final String propertyName = rs.getString("PROPERTY_NAME");
                final String propertyValue = rs.getString("PROPERTY_VALUE");

                clientProperties.put(propertyName, propertyValue);
            }

            return clientProperties;
        }
    }
}
