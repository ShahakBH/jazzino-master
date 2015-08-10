package com.yazino.platform.player.persistence;

import com.google.common.base.Optional;
import com.yazino.platform.Partner;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.player.*;
import com.yazino.platform.player.util.TestModeGuard;
import com.yazino.platform.util.BigDecimals;
import com.yazino.platform.util.community.AvatarTokeniser;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Repository("playerProfileDao")
public class JDBCPlayerProfileDao implements PlayerProfileDao {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCPlayerProfileDao.class);

    private static final RowMapper<PlayerProfile> USER_PROFILE_MAPPER = new PlayerProfileMapper();

    private static final String INSERT_OR_UPDATE_SQL =
            "INSERT INTO LOBBY_USER "
                    + "(EMAIL_ADDRESS,DISPLAY_NAME,REAL_NAME,GENDER,COUNTRY,FIRST_NAME,LAST_NAME,"
                    + "DATE_OF_BIRTH, PLAYER_ID,REFERRAL_ID, PROVIDER_NAME, RPX_PROVIDER,"
                    + " EXTERNAL_ID, VERIFICATION_IDENTIFIER, SYNC_PROFILE, TSREG, EMAIL_OPT_IN, GUEST_STATUS, PARTNER_ID) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                    + "ON DUPLICATE KEY UPDATE EMAIL_ADDRESS=VALUES(EMAIL_ADDRESS), DISPLAY_NAME=VALUES(DISPLAY_NAME),"
                    + "REAL_NAME=VALUES(REAL_NAME), GENDER=VALUES(GENDER), COUNTRY=VALUES(COUNTRY), FIRST_NAME=VALUES(FIRST_NAME),"
                    + "LAST_NAME=VALUES(LAST_NAME), DATE_OF_BIRTH=VALUES(DATE_OF_BIRTH),"
                    + "PLAYER_ID=VALUES(PLAYER_ID), SYNC_PROFILE=VALUES(SYNC_PROFILE), PROVIDER_NAME=VALUES(PROVIDER_NAME),"
                    + "RPX_PROVIDER=VALUES(RPX_PROVIDER), EXTERNAL_ID=VALUES(EXTERNAL_ID), "
                    + "VERIFICATION_IDENTIFIER=VALUES(VERIFICATION_IDENTIFIER), GUEST_STATUS=VALUES(GUEST_STATUS),"
                    + "PARTNER_ID=VALUES(PARTNER_ID)";

    private static final String SELECT_BY_PROVIDER_NAME_AND_EXTERNAL_ID
            = "SELECT * FROM LOBBY_USER WHERE PROVIDER_NAME=? AND EXTERNAL_ID=? ORDER BY PLAYER_ID DESC";

    private static final String SELECT_BY_EMAIL_ADDRESS = "SELECT * FROM LOBBY_USER WHERE EMAIL_ADDRESS=?";

    private static final String SELECT_BY_PLAYER_ID = "SELECT * FROM LOBBY_USER WHERE PLAYER_ID=?";

    private static final String UPDATE_STATUS_SQL = "UPDATE LOBBY_USER SET STATUS=? WHERE PLAYER_ID=?";

    private static final String UPDATE_ROLE_SQL = "UPDATE PLAYER SET IS_INSIDER=? WHERE PLAYER_ID=?";

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM LOBBY_USER";

    private static final String SELECT_REGISTERED_BY_EMAIL = "SELECT EMAIL_ADDRESS, PLAYER_ID FROM LOBBY_USER "
            + "WHERE EMAIL_ADDRESS IN (:emailAddresses)";

    private static final String SELECT_REGISTERED_BY_EXTERNAL_IDS = "SELECT EXTERNAL_ID, PLAYER_ID FROM LOBBY_USER "
            + "WHERE PROVIDER_NAME = :providerName AND EXTERNAL_ID IN (:externalIds)";

    private static final String DEREGISTER_BY_PROVIDER_NAME_AND_EXTERNAL_ID_SQL =
            "UPDATE LOBBY_USER SET EXTERNAL_ID=CONCAT('invalidated_', EXTERNAL_ID) "
                    + "WHERE PROVIDER_NAME = :providerName AND EXTERNAL_ID = :externalId";

    private static final String SQL_SEARCH_BY_EMAIL
            = "SELECT SQL_NO_CACHE SQL_CALC_FOUND_ROWS L.PLAYER_ID,L.EMAIL_ADDRESS,L.REAL_NAME,L.DISPLAY_NAME,L.PROVIDER_NAME,L.STATUS,P.PICTURE_LOCATION,P.IS_INSIDER "
            + " FROM LOBBY_USER L "
            + " LEFT JOIN PLAYER P ON L.PLAYER_ID = P.PLAYER_ID "
            + " WHERE L.EMAIL_ADDRESS LIKE ? ORDER BY L.REAL_NAME LIMIT ? OFFSET ?";
    private static final String SQL_SEARCH_BY_NAME
            = "SELECT SQL_NO_CACHE SQL_CALC_FOUND_ROWS L.PLAYER_ID,L.EMAIL_ADDRESS,L.REAL_NAME,L.DISPLAY_NAME,L.PROVIDER_NAME,L.STATUS,P.PICTURE_LOCATION,P.IS_INSIDER "
            + " FROM LOBBY_USER L "
            + " LEFT JOIN PLAYER P ON L.PLAYER_ID = P.PLAYER_ID "
            + " WHERE L.REAL_NAME LIKE ? OR L.DISPLAY_NAME LIKE ? ORDER BY L.REAL_NAME LIMIT ? OFFSET ?";
    private static final String SQL_FOUND_ROWS = "SELECT FOUND_ROWS()";

    private static final String SQL_SELECT_SUMMARY = "SELECT P.PLAYER_ID, "
            + "  P.ACCOUNT_ID, "
            + "  P.PICTURE_LOCATION, "
            + "  P.TS_LAST_PLAYED, "
            + "  P.LEVEL, "
            + "  P.IS_INSIDER, "
            + "  P.TAGS, "
            + "  A.BALANCE, "
            + "  L.TSREG, "
            + "  L.REAL_NAME, "
            + "  L.EMAIL_ADDRESS, "
            + "  L.DISPLAY_NAME, "
            + "  L.PROVIDER_NAME, "
            + "  L.GENDER, "
            + "  L.EXTERNAL_ID, "
            + "  L.COUNTRY, "
            + "  L.STATUS, "
            + "  SUM(S.PURCHASE_AMOUNT*(S.PURCHASE_CURRENCY ='USD')) pUSD, "
            + "  SUM(S.PURCHASE_AMOUNT*(S.PURCHASE_CURRENCY='GBP')) pGBP, "
            + "  SUM(S.PURCHASE_AMOUNT*(S.PURCHASE_CURRENCY ='EUR')) pEUR, "
            + "  SUM(S.PURCHASE_AMOUNT*(S.PURCHASE_CURRENCY='AUD')) pAUD, "
            + "  SUM(S.PURCHASE_AMOUNT*(S.PURCHASE_CURRENCY='CAD')) pCAD, "
            + "  SUM(S.CHIPS_AMOUNT) CHIPS "
            + "FROM LOBBY_USER L "
            + "  LEFT JOIN PLAYER P ON L.PLAYER_ID = P.PLAYER_ID "
            + "  LEFT JOIN ACCOUNT A ON P.ACCOUNT_ID = A.ACCOUNT_ID "
            + "  LEFT JOIN ACCOUNT_STATEMENT S ON P.ACCOUNT_ID = S.ACCOUNT_ID AND S.TRANSACTION_STATUS IN ('SUCCESS','AUTHORISED','SETTLED') "
            + "WHERE L.PLAYER_ID = ? "
            + "GROUP BY P.PLAYER_ID, P.ACCOUNT_ID, P.PICTURE_LOCATION, P.TS_LAST_UPDATE, P.LEVEL, P.IS_INSIDER, A.BALANCE, "
            + "  L.TSREG,L.REAL_NAME, L.EMAIL_ADDRESS, L.DISPLAY_NAME,L.PROVIDER_NAME, "
            + "  L.GENDER, L.EXTERNAL_ID, L.COUNTRY, L.STATUS";

    private static final String AUDIT_STATUS_CHANGE_SQL
            = "INSERT INTO PLAYER_PROFILE_STATUS_AUDIT (PLAYER_ID,OLD_STATUS,NEW_STATUS,CHANGED_BY,REASON)"
            + " VALUES (?,(SELECT STATUS FROM LOBBY_USER WHERE PLAYER_ID=?),?,?,?)";

    private static final String AUDIT_RECORDS_SQL = "SELECT * FROM PLAYER_PROFILE_STATUS_AUDIT WHERE PLAYER_ID=?";

    private static final String SQL_SELECT_EXTERNAL_IDS
            = "SELECT PLAYER_ID FROM LOBBY_USER lu INNER JOIN TMP_FRIEND_EXTERNAL_IDS eids ON lu.EXTERNAL_ID = eids.EXTERNAL_ID WHERE PROVIDER_NAME = ?";
    private static final String SQL_CREATE_TEMP_EXTERNAL_ID_TABLE = "CREATE TEMPORARY TABLE IF NOT EXISTS TMP_FRIEND_EXTERNAL_IDS (EXTERNAL_ID VARCHAR(255) NOT NULL PRIMARY KEY)";
    private static final String SQL_DROP_TEMP_EXTERNAL_ID_TABLE = "DROP TEMPORARY TABLE IF EXISTS TMP_FRIEND_EXTERNAL_IDS";

    private static final String SQL_GET_DISPLAY_NAMES = "SELECT PLAYER_ID,NAME FROM PLAYER WHERE PLAYER_ID IN (%s)";

    private final RowMapper<PlayerSearchResult> searchResultMapper = new PlayerSearchResultMapper();
    private final RowMapper<PlayerSummary> playerSummaryMapper = new PlayerSummaryMapper();
    private final RowMapper<PlayerProfileAudit> playerProfileAuditMapper = new PlayerProfileAuditMapper();

    @Autowired
    private TestModeGuard testModeGuard;

    private final AvatarTokeniser avatarTokeniser;
    private final JdbcTemplate template;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    JDBCPlayerProfileDao() {
        // CGLib constructor
        avatarTokeniser = null;
        template = null;
        namedParameterJdbcTemplate = null;
    }

    @Autowired
    public JDBCPlayerProfileDao(@Qualifier("jdbcTemplate") final JdbcTemplate template,
                                final AvatarTokeniser avatarTokeniser) {
        notNull(template, "template is null");
        notNull(avatarTokeniser, "avatarTokeniser may not be null");

        this.avatarTokeniser = avatarTokeniser;
        this.template = template;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(template);
    }

    @Override
    public void save(final PlayerProfile userProfile) {
        notNull(userProfile, "userProfile is null");

        LOG.debug("Saving userProfile {}", userProfile);

        template.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(INSERT_OR_UPDATE_SQL);
                int nextArgIndex = 1;
                st.setString(nextArgIndex++, userProfile.getEmailAddress());
                st.setString(nextArgIndex++, userProfile.getDisplayName());
                st.setString(nextArgIndex++, userProfile.getRealName());
                if (userProfile.getGender() != null) {
                    st.setString(nextArgIndex++, userProfile.getGender().getId());
                } else {
                    st.setNull(nextArgIndex++, Types.VARCHAR);
                }
                st.setString(nextArgIndex++, userProfile.getCountry());
                st.setString(nextArgIndex++, userProfile.getFirstName());
                st.setString(nextArgIndex++, userProfile.getLastName());
                if (userProfile.getDateOfBirth() != null) {
                    st.setDate(nextArgIndex++, new Date(userProfile.getDateOfBirth().getMillis()));
                } else {
                    st.setNull(nextArgIndex++, Types.DATE);
                }
                st.setBigDecimal(nextArgIndex++, userProfile.getPlayerId());

                st.setString(nextArgIndex++, userProfile.getReferralIdentifier());
                st.setString(nextArgIndex++, userProfile.getProviderName());
                if (userProfile.getRpxProvider() != null) {
                    st.setString(nextArgIndex++, userProfile.getRpxProvider());
                } else {
                    st.setString(nextArgIndex++, userProfile.getProviderName());
                }
                st.setString(nextArgIndex++, userProfile.getExternalId());
                st.setString(nextArgIndex++, userProfile.getVerificationIdentifier());
                st.setBoolean(nextArgIndex++, userProfile.isSyncProfile());
                if (userProfile.getRegistrationTime() != null) {
                    st.setTimestamp(nextArgIndex++, new Timestamp(userProfile.getRegistrationTime().getMillis()));
                } else {
                    st.setNull(nextArgIndex++, Types.TIMESTAMP);
                }
                if (userProfile.getOptIn() != null) {
                    st.setBoolean(nextArgIndex++, userProfile.getOptIn());
                } else {
                    st.setNull(nextArgIndex++, Types.BOOLEAN);
                }
                st.setString(nextArgIndex++, userProfile.getGuestStatus().getId());
                st.setString(nextArgIndex++, userProfile.getPartnerId().name());
                return st;
            }
        });

    }

    @Override
    public int count() {
        return template.queryForObject(COUNT_SQL, Integer.class);
    }

    @Override
    public PlayerProfile findByProviderNameAndExternalId(final String providerName,
                                                         final String externalId) {
        notBlank(providerName, "providerName is blank");
        notBlank(externalId, "externalId is blank");

        final List<PlayerProfile> results = template.query(SELECT_BY_PROVIDER_NAME_AND_EXTERNAL_ID,
                new Object[]{providerName, externalId}, USER_PROFILE_MAPPER);
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    @Override
    public PlayerProfile findByPlayerId(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        final List<PlayerProfile> list = template.query(
                new SimpleQueryCreator(SELECT_BY_PLAYER_ID, playerId), USER_PROFILE_MAPPER);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            LOG.warn("querying {} with playerId {} yielded {} results (expected 0 or 1).", SELECT_BY_PLAYER_ID, playerId, list.size());
        }
        return list.get(0);
    }

    @Override
    @Transactional
    public Set<BigDecimal> findPlayerIdsByProviderNameAndExternalIds(final Set<String> externalIds,
                                                                     final String providerName) {
        notBlank(providerName, "providerName cannot be blank");
        if (externalIds == null || externalIds.isEmpty()) {
            return Collections.emptySet();
        }

        final StringBuilder insertSql = new StringBuilder("INSERT INTO TMP_FRIEND_EXTERNAL_IDS VALUES ");
        final Iterator<String> idIterator = externalIds.iterator();
        while (idIterator.hasNext()) {
            insertSql.append("(?)");
            idIterator.next();
            if (idIterator.hasNext()) {
                insertSql.append(',');
            }
        }

        template.update(SQL_CREATE_TEMP_EXTERNAL_ID_TABLE);
        final List<BigDecimal> rawPlayerIds;
        try {
            template.update(insertSql.toString(), externalIds.toArray(new String[externalIds.size()]));
            rawPlayerIds = template.queryForList(SQL_SELECT_EXTERNAL_IDS, BigDecimal.class, providerName);
        } finally {
            template.update(SQL_DROP_TEMP_EXTERNAL_ID_TABLE);
        }

        final HashSet<BigDecimal> playerIds = new HashSet<>();
        for (BigDecimal unstrippedPlayerId : rawPlayerIds) {
            playerIds.add(BigDecimals.strip(unstrippedPlayerId));
        }
        return playerIds;
    }

    @Override
    public PlayerProfile findByEmailAddress(final String emailAddress) {
        if (StringUtils.isBlank(emailAddress)) {
            return null;
        }
        final List<PlayerProfile> results = template.query(SELECT_BY_EMAIL_ADDRESS,
                USER_PROFILE_MAPPER, emailAddress);
        if (results == null || results.size() != 1) {
            return null;
        }
        return results.get(0);
    }

    @Override
    @Transactional
    public void updateStatus(final BigDecimal playerId,
                             final PlayerProfileStatus newStatus,
                             final String changedBy,
                             final String reason) {
        notNull(playerId, "playerId may not be null");
        notNull(newStatus, "newStatus may not be null");
        notNull(changedBy, "changedBy may not be null");
        notNull(reason, "reason may not be null");

        LOG.debug("Updating status for playerId={} to newStatus={} by={} with reason={}", playerId, newStatus, changedBy, reason);

        try {
            template.update(AUDIT_STATUS_CHANGE_SQL, playerId, playerId, newStatus.getId(), changedBy, reason);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(format(
                    "Failed to change status to %s. User[playerId=%s] does not exist in database", newStatus, playerId));
        }

        template.update(UPDATE_STATUS_SQL, newStatus.getId(), playerId);
    }

    @Override
    public void updateRole(final BigDecimal playerId,
                           final PlayerProfileRole newRole) {
        notNull(playerId, "playerId may not be null");
        notNull(newRole, "newRole may not be null");

        LOG.debug("Updating role for playerId={} to newRole={}", playerId, newRole);

        boolean insider = false;
        if (newRole == PlayerProfileRole.INSIDER) {
            insider = true;
        }
        template.update(UPDATE_ROLE_SQL, insider, playerId);
    }

    @Override
    public List<PlayerProfileAudit> findAuditRecordsFor(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        final List<PlayerProfileAudit> results = template.query(AUDIT_RECORDS_SQL, playerProfileAuditMapper, playerId);
        Collections.sort(results);
        return results;
    }

    @Override
    public Map<String, BigDecimal> findRegisteredEmailAddresses(final String... candidateEmailAddresses) {
        if (ArrayUtils.isEmpty(candidateEmailAddresses)) {
            return Collections.emptyMap();
        }

        final Map<String, Object> parameters = new HashMap<>(1);
        parameters.put("emailAddresses", asList(candidateEmailAddresses));

        final Map<String, BigDecimal> matches = new HashMap<>();

        namedParameterJdbcTemplate.query(SELECT_REGISTERED_BY_EMAIL, parameters, new RowCallbackHandler() {
            public void processRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
                String emailAddress = resultSet.getString(1);
                BigDecimal playerId = BigDecimals.strip(resultSet.getBigDecimal(2));
                matches.put(emailAddress, playerId);
            }
        });
        return matches;
    }

    @Override
    public Map<String, BigDecimal> findRegisteredExternalIds(final String providerName, final String... externalIds) {
        if (ArrayUtils.isEmpty(externalIds)) {
            return Collections.emptyMap();
        }

        final Map<String, Object> parameters = new HashMap<>(2);
        parameters.put("providerName", providerName);
        parameters.put("externalIds", asList(externalIds));

        final Map<String, BigDecimal> matches = new HashMap<>();

        namedParameterJdbcTemplate.query(SELECT_REGISTERED_BY_EXTERNAL_IDS, parameters, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet resultSet) throws SQLException {
                String externalId = resultSet.getString(1);
                BigDecimal playerId = BigDecimals.strip(resultSet.getBigDecimal(2));
                matches.put(externalId, playerId);
            }
        });

        return matches;
    }

    @Override
    public void invalidateExternalId(final String providerName, final String externalId) {
        testModeGuard.assertTestModeEnabled();
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("providerName", providerName);
        parameters.put("externalId", externalId);
        final int updateCount =
                namedParameterJdbcTemplate.update(DEREGISTER_BY_PROVIDER_NAME_AND_EXTERNAL_ID_SQL, parameters);
        if (updateCount != 1) {
            throw new RuntimeException(format(
                    "Unable to deregister player (providerName='%s', externalId='%s')", providerName, externalId));
        }
    }

    @Override
    public void invalidateGuestPlayer(final String email) {
        testModeGuard.assertTestModeEnabled();
        final Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("email", email);
        queryParams.put("newEmail", format("guest.disabled_%s@yazino.com", new DateTime().getMillis()));
        unlinkGuestUsersFromDatabase(email, queryParams);
    }

    private void unlinkGuestUsersFromDatabase(final String email, final Map<String, Object> parameters) {
        final int lobbyUserRecordCount =
                namedParameterJdbcTemplate.update("UPDATE LOBBY_USER set email_address= :newEmail WHERE email_address = :email", parameters);
        if (lobbyUserRecordCount < 1) {
            throw new RuntimeException(format(
                    "Unable to de-register guest user from lobby_user(email_address='%s')", email));
        }

        final int yazinoLoginRecordCount =
                namedParameterJdbcTemplate.update("UPDATE YAZINO_LOGIN set email_address= :newEmail WHERE email_address = :email", parameters);
        if (yazinoLoginRecordCount < 1) {
            throw new RuntimeException(format(
                    "Unable to de-register guest user from yazino_login(email_address='%s')", email));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedData<PlayerSearchResult> searchByEmailAddress(final String emailAddress,
                                                              final int page,
                                                              final int pageSize) {
        if (emailAddress == null) {
            return PagedData.empty();
        }

        final List<PlayerSearchResult> searchResults = template.query(
                SQL_SEARCH_BY_EMAIL, searchResultMapper, emailAddress, pageSize, page * pageSize);
        final int totalRows = template.queryForObject(SQL_FOUND_ROWS, Integer.class);
        return new PagedData<>(page * pageSize, searchResults.size(), totalRows, searchResults);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedData<PlayerSearchResult> searchByRealOrDisplayName(final String name,
                                                                   final int page,
                                                                   final int pageSize) {
        if (name == null) {
            return PagedData.empty();
        }

        final List<PlayerSearchResult> searchResults = template.query(
                SQL_SEARCH_BY_NAME, searchResultMapper, name, name, pageSize, page * pageSize);
        final int totalRows = template.queryForObject(SQL_FOUND_ROWS, Integer.class);
        return new PagedData<>(page * pageSize, searchResults.size(), totalRows, searchResults);
    }

    @Override
    public Optional<PlayerSummary> findSummaryById(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        final List<PlayerSummary> results = template.query(SQL_SELECT_SUMMARY, playerSummaryMapper, playerId);
        if (!results.isEmpty()) {
            return Optional.fromNullable(results.get(0));
        }
        return Optional.absent();
    }

    @Override
    public Map<BigDecimal, String> findDisplayNamesByIds(final Set<BigDecimal> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return template.query(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                final PreparedStatement stmt = con.prepareStatement(String.format(SQL_GET_DISPLAY_NAMES, inClauseFor(playerIds.size())));
                int parameter = 1;
                for (BigDecimal playerId : playerIds) {
                    stmt.setBigDecimal(parameter++, playerId);
                }
                return stmt;
            }
        }, new ResultSetExtractor<Map<BigDecimal, String>>() {
            @Override
            public Map<BigDecimal, String> extractData(final ResultSet rs) throws SQLException, DataAccessException {
                final Map<BigDecimal, String> idsToDisplayNames = new HashMap<>();
                while (rs.next()) {
                    idsToDisplayNames.put(BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")), rs.getString("NAME"));
                }
                return idsToDisplayNames;
            }
        });
    }

    private String inClauseFor(final int size) {
        final StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            if (i != 0) {
                inClause.append(",");
            }
            inClause.append("?");
        }
        return inClause.toString();
    }

    private PlayerProfileRole roleFor(final boolean insider) {
        if (insider) {
            return PlayerProfileRole.INSIDER;
        }
        return PlayerProfileRole.CUSTOMER;
    }

    private class PlayerProfileAuditMapper implements RowMapper<PlayerProfileAudit> {
        @Override
        public PlayerProfileAudit mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new PlayerProfileAudit(BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")),
                    PlayerProfileStatus.forId(rs.getString("OLD_STATUS")),
                    PlayerProfileStatus.forId(rs.getString("NEW_STATUS")),
                    rs.getString("CHANGED_BY"),
                    rs.getString("REASON"),
                    new DateTime(rs.getTimestamp("CHANGED_TS")));
        }
    }

    private class PlayerSearchResultMapper implements RowMapper<PlayerSearchResult> {
        @Override
        public PlayerSearchResult mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new PlayerSearchResult(BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")),
                    rs.getString("EMAIL_ADDRESS"),
                    rs.getString("REAL_NAME"),
                    rs.getString("DISPLAY_NAME"),
                    rs.getString("PROVIDER_NAME"),
                    avatarTokeniser.detokenise(rs.getString("PICTURE_LOCATION")),
                    PlayerProfileStatus.forId(rs.getString("STATUS")),
                    roleFor(rs.getBoolean("IS_INSIDER")));
        }
    }

    private class PlayerSummaryMapper implements RowMapper<PlayerSummary> {
        @Override
        public PlayerSummary mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final Map<String, BigDecimal> purchasesByCurrency = new HashMap<>();
            for (final String currency : new String[]{"USD", "GBP", "EUR", "AUD", "CAD"}) {
                final BigDecimal value = rs.getBigDecimal("p" + currency);
                if (value != null && BigDecimal.ZERO.compareTo(value) != 0) {
                    purchasesByCurrency.put(currency, value);
                }
            }
            return new PlayerSummary(BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")),
                    BigDecimals.strip(rs.getBigDecimal("ACCOUNT_ID")),
                    avatarTokeniser.detokenise(rs.getString("PICTURE_LOCATION")),
                    asDateTime(rs, "TS_LAST_PLAYED"),
                    asDateTime(rs, "TSREG"),
                    defaultIfNull(rs.getBigDecimal("BALANCE"), BigDecimal.ZERO),
                    rs.getString("REAL_NAME"),
                    rs.getString("DISPLAY_NAME"),
                    rs.getString("EMAIL_ADDRESS"),
                    rs.getString("PROVIDER_NAME"),
                    rs.getString("EXTERNAL_ID"),
                    rs.getString("COUNTRY"),
                    Gender.getById(rs.getString("GENDER")),
                    PlayerProfileStatus.forId(rs.getString("STATUS")),
                    roleFor(rs.getBoolean("IS_INSIDER")),
                    defaultIfNull(rs.getBigDecimal("CHIPS"), BigDecimal.ZERO),
                    purchasesByCurrency,
                    parseLevels(rs.getString("LEVEL")),
                    parseTags(rs.getString("TAGS")));
        }

        private Set<String> parseTags(final String tagsAsString) {
            final Set<String> tags = new HashSet<>();
            if (tagsAsString != null) {
                tags.addAll(asList(tagsAsString.split(",")));
            }
            return tags;
        }

        private Map<String, Integer> parseLevels(final String levelRecord) {
            final Map<String, Integer> levels = new HashMap<>();
            if (levelRecord == null) {
                return levels;
            }

            for (String gameRecord : levelRecord.split("\n")) {
                if (gameRecord == null || gameRecord.trim().length() == 0) {
                    continue;
                }
                final String[] fields = gameRecord.split("\t");
                if (fields.length > 2) {
                    levels.put(fields[0], Integer.parseInt(fields[1]));
                }
            }
            return levels;
        }

        private DateTime asDateTime(final ResultSet resultSet, final String columnName) {
            // the MySQL driver (with the default options) will throw an exception on a 0 date. This occurs even
            // if you fetch it as a string. Unless you require the driver option at all times you need
            // to handle the exception. http://dev.mysql.com/doc/refman/5.1/en/connector-j-installing-upgrading.html

            try {
                final Timestamp timestamp = resultSet.getTimestamp(columnName);
                if (timestamp != null) {
                    return new DateTime(timestamp);
                }
            } catch (SQLException e) {
                LOG.debug("Received invalid timestamp from MySQL", e);
            }

            return null;
        }
    }

    static class PlayerProfileMapper implements RowMapper<PlayerProfile> {
        @Override
        public PlayerProfile mapRow(final ResultSet rs,
                                    final int i) throws SQLException {
            final BigDecimal playerId = BigDecimals.strip(rs.getBigDecimal("PLAYER_ID"));
            final String emailAddress = rs.getString("EMAIL_ADDRESS");
            final String displayName = rs.getString("DISPLAY_NAME");
            final String realName = rs.getString("REAL_NAME");
            final String country = rs.getString("COUNTRY");
            final String firstName = rs.getString("FIRST_NAME");
            final String lastName = rs.getString("LAST_NAME");
            final Timestamp timestamp = rs.getTimestamp("DATE_OF_BIRTH");
            DateTime dateOfBirth = null;
            if (timestamp != null) {
                dateOfBirth = new DateTime(timestamp.getTime());
            }
            final String genderId = rs.getString("GENDER");
            Gender gender = null;
            if (genderId != null) {
                gender = Gender.getById(genderId);
            }
            final String guestStatusId = rs.getString("GUEST_STATUS");
            GuestStatus guestStatus = null;
            if (guestStatusId != null) {
                guestStatus = GuestStatus.getById(guestStatusId);
            }
            final String referralId = rs.getString("REFERRAL_ID");
            final String providerName = rs.getString("PROVIDER_NAME");
            final String rpxProvider = rs.getString("RPX_PROVIDER");
            final String externalId = rs.getString("EXTERNAL_ID");
            final boolean syncProfile = rs.getBoolean("SYNC_PROFILE");
            final Timestamp registrationTimestamp = rs.getTimestamp("TSREG");
            DateTime registrationTime = null;
            if (registrationTimestamp != null) {
                registrationTime = new DateTime(registrationTimestamp.getTime());
            }

            final PlayerProfile result = new PlayerProfile(playerId, emailAddress,
                    displayName, realName, gender, country, firstName, lastName, dateOfBirth,
                    referralId, providerName, rpxProvider, externalId, syncProfile);
            result.setVerificationIdentifier(rs.getString("VERIFICATION_IDENTIFIER"));
            result.setStatus(PlayerProfileStatus.forId(rs.getString("STATUS")));
            result.setPartnerId(Partner.parse(rs.getString("PARTNER_ID")));
            result.setRegistrationTime(registrationTime);
            result.setGuestStatus(guestStatus);
            return result;
        }
    }

}
