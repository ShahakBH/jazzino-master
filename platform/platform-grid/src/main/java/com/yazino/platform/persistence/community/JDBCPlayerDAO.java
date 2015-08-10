package com.yazino.platform.persistence.community;

import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.util.BigDecimals;
import com.yazino.platform.util.community.AvatarTokeniser;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("playerDao")
public class JDBCPlayerDAO implements PlayerDAO {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCPlayerDAO.class);

    private static final String FIELD_DELIMITER = "\t";
    private static final String RECORD_DELIMITER = "\n";
    private static final String SELECT_PLAYERS = "SELECT * FROM PLAYER";
    private static final String INSERT_OR_UPDATE_PLAYER = "INSERT INTO PLAYER "
            + "(PLAYER_ID, NAME, ACCOUNT_ID, RELATIONSHIPS, PICTURE_LOCATION, "
            + "PREFERRED_CURRENCY, PREFERRED_PAYMENT_METHOD, TSCREATED, ts_last_played, tags) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?)"
            + "ON DUPLICATE KEY UPDATE NAME=VALUES(NAME), RELATIONSHIPS=VALUES(RELATIONSHIPS),"
            + "PICTURE_LOCATION=VALUES(PICTURE_LOCATION), PREFERRED_CURRENCY=VALUES(PREFERRED_CURRENCY),"
            + "TS_LAST_PLAYED=VALUES(TS_LAST_PLAYED), TAGS=VALUES(TAGS)";
    private static final String SELECT_FOUND_ROWS = "SELECT FOUND_ROWS()";
    private static final String FIND_BY_NAME = "SELECT SQL_CALC_FOUND_ROWS * FROM PLAYER "
            + "WHERE NAME LIKE ? ORDER BY NAME ASC LIMIT ?,?";
    private static final String FIND_BY_NAME_AND_EXCLUDE = "SELECT SQL_CALC_FOUND_ROWS * FROM PLAYER "
            + "WHERE PLAYER_ID <> ? AND NAME LIKE ? ORDER BY NAME ASC LIMIT ?,?";
    private static final String SELECT_BY_ID = "SELECT * FROM PLAYER WHERE PLAYER_ID=?";
    private static final String SELECT_BY_IDS = "SELECT * FROM PLAYER WHERE PLAYER_ID IN ";
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final RowMapper<Player> playerRowMapper = new PlayerRowMapper();

    private final AvatarTokeniser avatarTokeniser;
    private final JdbcTemplate template;

    private int pageSize = DEFAULT_PAGE_SIZE;

    @Autowired
    public JDBCPlayerDAO(@Qualifier("jdbcTemplate") final JdbcTemplate template,
                         final AvatarTokeniser avatarTokeniser) {
        notNull(template, "template may not be null");
        notNull(avatarTokeniser, "avatarTokeniser may not be null");

        this.template = template;
        this.avatarTokeniser = avatarTokeniser;
    }

    public void setPageSize(final int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }
        this.pageSize = pageSize;
    }

    public void save(final Player player) {
        notNull(player, "Player may not be null");
        notNull(player.getPlayerId(), "Player ID may not be null");
        notNull(player.getAccountId(), "Player account ID may not be null");
        notNull(player.getCreationTime(), "Player creation time is null");
        LOG.debug("Saving player {}", player);
        saveBasicProperties(player);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Player saved with ID {}: {}", player.getPlayerId(), player);
        }
    }


    @SuppressWarnings("unchecked")
    public Set<Player> findAll() {
        LOG.debug("Find all players");
        final List<Player> players = template.query(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                return conn.prepareStatement(SELECT_PLAYERS);
            }
        }, playerRowMapper);

        LOG.debug("Find all player relationships");

        if (players != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loaded {} players!", players.size());
            }
            return new HashSet<Player>(players);
        }
        return Collections.emptySet();
    }

    @Override
    public Player findById(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        try {
            return template.queryForObject(SELECT_BY_ID, new Object[]{playerId}, playerRowMapper);

        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public Collection<Player> findByIds(final Set<BigDecimal> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Collections.emptySet();
        }

        return template.query(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                final PreparedStatement stmt = con.prepareStatement(inStatementFor(SELECT_BY_IDS, playerIds.size()));
                int index = 1;
                for (BigDecimal playerId : playerIds) {
                    stmt.setBigDecimal(index++, playerId);
                }
                return stmt;
            }
        }, playerRowMapper);
    }

    private String inStatementFor(final String sqlStatement,
                                         final int argCount) {
        final StringBuilder sqlStmt = new StringBuilder(sqlStatement).append("(");
        for (int i = 0; i < argCount; ++i) {
            if (i == 0) {
                sqlStmt.append("?");
            } else {
                sqlStmt.append(",?");
            }
        }
        return sqlStmt.append(")").toString();
    }

    @Override
    public PagedData<Player> findByName(final String name,
                                        final int page,
                                        final BigDecimal playerIdToExclude) {
        notNull(name, "name may not be null");

        LOG.debug("Searching for player with exclusion={}; name={}, page={}, pageSize={}",
                playerIdToExclude, name, page, pageSize);

        final int lowerLimit = page * pageSize;
        final int upperLimit = lowerLimit + pageSize;
        final List<Player> queryResults;
        if (playerIdToExclude != null) {
            queryResults = template.query(FIND_BY_NAME_AND_EXCLUDE,
                    new Object[]{playerIdToExclude, name + "%", lowerLimit, upperLimit}, playerRowMapper);
        } else {
            queryResults = template.query(FIND_BY_NAME,
                    new Object[]{name + "%", lowerLimit, upperLimit}, playerRowMapper);
        }

        if (queryResults == null || queryResults.isEmpty()) {
            return new PagedData<Player>(lowerLimit, 0, 0, Collections.<Player>emptyList());
        }

        final int foundRows = template.queryForInt(SELECT_FOUND_ROWS);
        return new PagedData<Player>(lowerLimit, queryResults.size(), foundRows, queryResults);
    }

    @Override
    public void updateLastPlayedTs(final BigDecimal playerId, final DateTime lastPlayedTs) {
        template.update("UPDATE PLAYER SET ts_last_played = ? WHERE PLAYER_ID = ?", new Timestamp(lastPlayedTs.getMillis()), playerId);
    }

    private void saveBasicProperties(final Player player) {
        template.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(
                        INSERT_OR_UPDATE_PLAYER, Statement.NO_GENERATED_KEYS);
                int argIndex = 1;
                st.setBigDecimal(argIndex++, player.getPlayerId());
                st.setString(argIndex++, player.getName());
                st.setBigDecimal(argIndex++, player.getAccountId());
                st.setString(argIndex++, buildRelationshipsString(player.getRelationships()));
                st.setString(argIndex++, avatarTokeniser.tokenise(player.getPictureUrl()));

                if (player.getPaymentPreferences() != null && player.getPaymentPreferences().getCurrency() != null) {
                    st.setString(argIndex++, player.getPaymentPreferences().getCurrency().toString());
                } else {
                    st.setNull(argIndex++, java.sql.Types.VARCHAR);
                }

                if (player.getPaymentPreferences() != null
                        && player.getPaymentPreferences().getPaymentMethod() != null) {
                    st.setString(argIndex++, player.getPaymentPreferences().getPaymentMethod().toString());
                } else {
                    st.setNull(argIndex++, java.sql.Types.VARCHAR);
                }

                st.setTimestamp(argIndex++, new Timestamp(player.getCreationTime().getMillis()));
                st.setTimestamp(argIndex++, getNullOrTimestamp(player.getLastPlayed()));
                st.setString(argIndex++, StringUtils.join(player.getTags(), ","));
                return st;
            }
        });
    }

    private Timestamp getNullOrTimestamp(final DateTime dateTime) {
        if (dateTime == null) {
            return null;
        } else {
            return new Timestamp(dateTime.getMillis());
        }
    }

    private String buildRelationshipsString(final Map<BigDecimal, Relationship> relationships) {
        if (relationships == null || relationships.size() == 0) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        for (BigDecimal friendId : relationships.keySet()) {
            builder.append(friendId);
            builder.append(FIELD_DELIMITER);
            final Relationship relationship = relationships.get(friendId);
            builder.append(relationship.getNickname());
            builder.append(FIELD_DELIMITER);
            builder.append(relationship.getType());
            builder.append(RECORD_DELIMITER);
        }
        return builder.toString();
    }

    private Map<BigDecimal, Relationship> parseRelationshipsString(final String relationships) {
        if (StringUtils.isBlank(relationships)) {
            return null;
        }
        final Map<BigDecimal, Relationship> result = new HashMap<BigDecimal, Relationship>();
        final StringTokenizer records = new StringTokenizer(relationships, RECORD_DELIMITER);
        while (records.hasMoreTokens()) {
            final String record = records.nextToken();
            final String[] fields = record.split(FIELD_DELIMITER);
            RelationshipType type = null;
            try {
                type = RelationshipType.valueOf(fields[2]);
            } catch (Throwable t) {
                LOG.error("Unrecognised relationship Type {}", fields[2]);
            }
            if (type != null) {
                final BigDecimal friendId = new BigDecimal(fields[0]);
                final String nickname = fields[1];
                result.put(friendId, new Relationship(nickname, type));
            }
        }
        return result;
    }


    private class PlayerRowMapper implements RowMapper<Player> {
        public Player mapRow(final ResultSet rs,
                             final int rowNum) throws SQLException {
            final BigDecimal playerId = BigDecimals.strip(rs.getBigDecimal("PLAYER_ID"));
            final String name = rs.getString("NAME");
            final BigDecimal accountId = BigDecimals.strip(rs.getBigDecimal("ACCOUNT_ID"));
            final String pictureLocation = avatarTokeniser.detokenise(rs.getString("PICTURE_LOCATION"));
            Currency preferredCurrency = null;
            final String preferredCurrencyString = rs.getString("PREFERRED_CURRENCY");
            if (preferredCurrencyString != null) {
                try {
                    preferredCurrency = Currency.valueOf(preferredCurrencyString);
                } catch (IllegalArgumentException e) {
                    LOG.error("Illegal Preferred Currency {} Found for Player {}", preferredCurrency, playerId);
                }
            }
            PaymentPreferences.PaymentMethod paymentMethod = null;
            final String paymentMethodString = rs.getString("PREFERRED_PAYMENT_METHOD");
            if (paymentMethodString != null) {
                try {
                    paymentMethod = PaymentPreferences.PaymentMethod.valueOf(paymentMethodString);
                } catch (IllegalArgumentException e) {
                    LOG.error("Illegal Preferred payment method {} found for player {}", paymentMethodString, playerId);
                }

            }

            final PaymentPreferences paymentPreferences = new PaymentPreferences(preferredCurrency, paymentMethod);

            final DateTime tscreated = new DateTime(rs.getTimestamp("TSCREATED"));
            final DateTime lastPlayedTs = getDateTimeOrNull(rs.getTimestamp("ts_last_played"));

            final Player player = new Player(playerId, name,
                    accountId, pictureLocation, paymentPreferences, tscreated, lastPlayedTs);
            player.setRelationships(parseRelationshipsString(rs.getString("RELATIONSHIPS")));

            final String tags = rs.getString("TAGS");
            if (!StringUtils.isBlank(tags)) {
                player.setTags(new HashSet<String>(Arrays.asList(tags.split(","))));
            }
            return player;
        }

        private DateTime getDateTimeOrNull(final Timestamp lastPlayed) throws SQLException {
            if (lastPlayed == null) {
                return null;
            } else {
                return new DateTime(lastPlayed);
            }
        }
    }
}
