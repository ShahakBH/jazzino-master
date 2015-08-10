package com.yazino.platform.persistence.table;

import com.gigaspaces.datasource.DataIterator;
import com.thoughtworks.xstream.XStream;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.persistence.DataIterable;
import com.yazino.platform.persistence.ResultSetIterator;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.repository.table.GameVariationRepository;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.util.BigDecimals;
import com.yazino.platform.util.Visitor;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.GameType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notNull;

@Repository("tableDao")
public class JDBCTableDAO implements TableDAO, DataIterable<Table> {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCTableDAO.class);

    private static final String INSERT_TABLE_INFO = "INSERT INTO TABLE_INFO "
            + "(TABLE_ID,CURRENT_STATUS,GAME_ID,GAME_TYPE,STATUS,TS,"
            + " CLIENT_ID,GAME_VARIATION_TEMPLATE_ID,SHOW_IN_LOBBY,TABLE_NAME,TSCREATED,OWNER_ID,TAGS)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE_TABLE_INFO = "UPDATE TABLE_INFO SET CURRENT_STATUS=?,GAME_ID=?,"
            + "GAME_TYPE=?,STATUS=?,TS=?,CLIENT_ID=?,GAME_VARIATION_TEMPLATE_ID=?,SHOW_IN_LOBBY=?, OWNER_ID=?,TAGS=?"
            + " WHERE TABLE_ID=?";

    private static final String SELECT_TABLE = "SELECT t.*,gvt.name AS TEMPLATE_NAME "
            + "FROM TABLE_INFO t, GAME_VARIATION_TEMPLATE gvt "
            + "WHERE TABLE_ID=? AND t.game_variation_template_id=gvt.game_variation_template_id";

    private static final String VISIT_TABLE = "SELECT t.*,gvt.name AS TEMPLATE_NAME "
            + "FROM TABLE_INFO t, GAME_VARIATION_TEMPLATE gvt "
            + "WHERE t.STATUS=? AND t.game_variation_template_id=gvt.game_variation_template_id";

    private static final String SELECT_ALL_OPEN = "SELECT t.*,gvt.name AS TEMPLATE_NAME "
            + "FROM TABLE_INFO t, GAME_VARIATION_TEMPLATE gvt "
            + "WHERE t.STATUS='O' "
            + "AND t.OWNER_ID IS NOT NULL "
            + "AND t.game_variation_template_id = gvt.game_variation_template_id";

    private static XStream xstream = new XStream();

    private final RowMapper<Table> tableRowMapper = new TableRowMapper();

    private final ClientDAO clientDAO;
    private final GameVariationRepository gameTemplateRepository;
    private final GameRepository gameRepository;
    private final JdbcTemplate template;

    @Autowired
    public JDBCTableDAO(@Qualifier("jdbcTemplate") final JdbcTemplate jdbcTemplate,
                        final ClientDAO clientDAO,
                        final GameVariationRepository gameTemplateRepository,
                        final GameRepository gameRepository) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");
        notNull(clientDAO, "clientDAO may not be null");
        notNull(gameTemplateRepository, "gameTemplateRepository may not be null");
        notNull(gameRepository, "gameRepository may not be null");

        this.template = jdbcTemplate;
        this.clientDAO = clientDAO;
        this.gameTemplateRepository = gameTemplateRepository;
        this.gameRepository = gameRepository;
    }

    public Table findById(final BigDecimal id) {
        LOG.debug("entering getTableInfo {}", id);

        try {
            LOG.debug("loading table info {}", id);
            final Table table = template.queryForObject(SELECT_TABLE, tableRowMapper, id);
            gameTemplateRepository.populateProperties(table);

            return table;

        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public boolean save(final Table table) {
        notNull(table, "Table may not be null");
        notNull(table.getTableId(), "Table ID may not be null");

        LOG.debug("entering save {}", table);

        return saveOrUpdate(table);
    }

    private boolean saveOrUpdate(final Table table) {
        LOG.debug("entering saveOrUpdate {}" + table);
        final boolean showInLobby;
        if (table.getShowInLobby() == null) {
            showInLobby = true;
        } else {
            showInLobby = table.getShowInLobby();
        }

        if (table.getCreatedDateTime() == null || table.getLastUpdated() == null) {
            final DateTime currentTime = new DateTime();
            if (table.getCreatedDateTime() == null) {
                table.setCreatedDateTime(currentTime);
            }

            if (table.getLastUpdated() == null) {
                table.setLastUpdated(currentTime.getMillis());
            }
        }

        final Timestamp lastUpdatedTimestamp = new Timestamp(table.getLastUpdated());
        final int rowsUpdated = template.update(UPDATE_TABLE_INFO, "", table.getGameId(),
                table.getGameTypeId(), table.getTableStatus().getStatusName(), lastUpdatedTimestamp,
                table.getClientId(), table.getTemplateId(), showInLobby,
                table.getOwnerId(), asCommaSeparatedField(table.getTags()), table.getTableId());

        if (rowsUpdated == 0) {
            template.update(INSERT_TABLE_INFO, table.getTableId(), "", table.getGameId(),
                    table.getGameTypeId(), table.getTableStatus().getStatusName(), lastUpdatedTimestamp,
                    table.getClientId(), table.getTemplateId(), showInLobby, table.getTableName(),
                    new Timestamp(table.getCreatedDateTime().getMillis()),
                    table.getOwnerId(), asCommaSeparatedField(table.getTags()));
            return true;
        }

        return false;
    }

    @Override
    public DataIterator<Table> iterateAll() {
        final Map<String, Client> allClients = new HashMap<String, Client>();
        for (Client client : clientDAO.findAll()) {
            allClients.put(client.getClientId(), client);
        }
        final IteratorTableRowMapper rowMapper = new IteratorTableRowMapper(allClients);

        return new ResultSetIterator<Table>(template, new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                return con.prepareStatement(SELECT_ALL_OPEN);
            }
        }, rowMapper);
    }

    public void visitTables(final TableStatus status, final Visitor<Table> visitor) {
        template.query(VISIT_TABLE, new Object[]{status.getStatusName()},
                new RowCallbackHandler() {
                    public void processRow(final ResultSet rs) throws SQLException {
                        final Table table = tableRowMapper.mapRow(rs, 0);
                        try {
                            visitor.visit(table);
                        } catch (Exception e) {
                            LOG.error("Visitor caught an error", e);
                        }
                    }
                });
    }

    private String asCommaSeparatedField(final Set<String> field) {
        if (field == null || field.isEmpty()) {
            return null;
        }
        return StringUtils.join(field, ",");
    }

    private Set<String> asStringSet(final String csv) {
        if (csv == null || StringUtils.isBlank(csv)) {
            return null;
        }
        return new HashSet<String>(asList(csv.split(",")));
    }

    private class TableRowMapper implements RowMapper<Table> {
        public Table mapRow(final ResultSet rs,
                            final int rowNum)
                throws SQLException {
            LOG.debug("entering mapRow {}", rowNum);
            final Table table = new Table();
            final String xstreamString = rs.getString("CURRENT_STATUS");
            if (!StringUtils.isBlank(xstreamString)) {
                table.setCurrentGame((GameStatus) xstream.fromXML(xstreamString));
            }
            table.setGameId(rs.getLong("GAME_ID"));
            final GameType gameType = gameRepository.getGameTypeFor(rs.getString("GAME_TYPE"));

            table.setGameType(gameType);
            table.setTableId(BigDecimals.strip(rs.getBigDecimal("TABLE_ID")));
            table.setTableStatus(TableStatus.forStatusName(rs.getString("STATUS")));
            table.setTableName(rs.getString("TABLE_NAME"));
            table.setTemplateId(rs.getBigDecimal("GAME_VARIATION_TEMPLATE_ID"));
            table.setClientId(rs.getString("CLIENT_ID"));
            final Timestamp ts = rs.getTimestamp("TSCREATED");
            table.setCreatedDateTime(new DateTime(ts.getTime()));
            final Timestamp tsUpdated = rs.getTimestamp("TS");
            if (!rs.wasNull()) {
                table.setLastUpdated(tsUpdated.getTime());
            }
            table.setShowInLobby(rs.getBoolean("SHOW_IN_LOBBY"));
            table.setTemplateName(rs.getString("TEMPLATE_NAME"));
            table.setFull(false);
            table.setOwnerId(BigDecimals.strip(rs.getBigDecimal("OWNER_ID")));
            table.setTags(asStringSet(rs.getString("TAGS")));
            return table;
        }
    }

    private class IteratorTableRowMapper implements RowMapper<Table> {
        private final Map<String, Client> clients;

        public IteratorTableRowMapper(final Map<String, Client> clients) {
            this.clients = clients;
        }

        @Override
        public Table mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            LOG.debug("Entering iterator row mapper for row {}", rowNum);

            final Table table = tableRowMapper.mapRow(rs, rowNum);
            gameTemplateRepository.populateProperties(table);
            table.setClient(clients.get(table.getClientId()));

            return table;
        }
    }
}
