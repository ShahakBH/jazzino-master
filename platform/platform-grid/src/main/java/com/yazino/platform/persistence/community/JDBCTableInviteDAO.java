package com.yazino.platform.persistence.community;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.community.TableInvite;
import com.yazino.platform.persistence.DataIterable;
import com.yazino.platform.persistence.ResultSetIterator;
import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("tableInviteDao")
public class JDBCTableInviteDAO implements TableInviteDAO, DataIterable<TableInvite> {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCTableInviteDAO.class);

    private static final String INSERT_OR_UPDATE_TABLE_INVITE = "INSERT INTO TABLE_INVITE "
            + "(ID, PLAYER_ID,TABLE_ID,INVITE_TIME, OPEN) VALUES (?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE PLAYER_ID=VALUES(PLAYER_ID), TABLE_ID=VALUES(TABLE_ID), "
            + " INVITE_TIME=VALUES(INVITE_TIME), OPEN=VALUES(OPEN)";
    private static final String SELECT_ALL = "SELECT ID, PLAYER_ID, TABLE_ID, INVITE_TIME, OPEN FROM TABLE_INVITE WHERE OPEN = 1";

    private final TableInviteRowMapper rowMapper = new TableInviteRowMapper();
    private final JdbcTemplate template;

    @Autowired
    public JDBCTableInviteDAO(final JdbcTemplate template) {
        notNull(template, "template must not be null");
        this.template = template;
    }

    @Override
    public void save(final TableInvite tableInvite) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating tableInvite: " + tableInvite);
        }

        template.update(INSERT_OR_UPDATE_TABLE_INVITE, tableInvite.getId(),
                tableInvite.getPlayerId(), tableInvite.getTableId(),
                new Timestamp(tableInvite.getInviteTime().getMillis()), tableInvite.isOpen());
    }

    @Override
    public DataIterator<TableInvite> iterateAll() {
        return new ResultSetIterator<TableInvite>(template, new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                return con.prepareStatement(SELECT_ALL);
            }
        }, rowMapper);
    }

    static final class TableInviteRowMapper implements RowMapper<TableInvite> {

        @Override
        public TableInvite mapRow(final ResultSet rs,
                                  final int rowNum) throws SQLException {
            final BigDecimal id = rs.getBigDecimal("ID");
            final BigDecimal playerId = BigDecimals.strip(rs.getBigDecimal("PLAYER_ID"));
            final BigDecimal tableId = BigDecimals.strip(rs.getBigDecimal("TABLE_ID"));
            final Timestamp inviteTime = rs.getTimestamp("INVITE_TIME");
            final Boolean open = rs.getBoolean("OPEN");

            final TableInvite invite = new TableInvite(playerId, tableId, new DateTime(inviteTime.getTime()), open);
            invite.setId(id);
            return invite;
        }
    }
}
