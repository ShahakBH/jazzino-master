package com.yazino.platform.player.persistence;

import com.google.common.base.Joiner;
import com.yazino.platform.player.PasswordType;
import com.yazino.platform.player.YazinoLogin;
import com.yazino.platform.util.BigDecimals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("yazinoLoginDao")
public class JDBCYazinoLoginDao implements YazinoLoginDao {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCYazinoLoginDao.class);

    private static final String INSERT_OR_UPDATE_SQL = "INSERT INTO YAZINO_LOGIN "
            + "(PLAYER_ID, EMAIL_ADDRESS, PASSWORD_HASH, PASSWORD_TYPE, SALT, LOGIN_ATTEMPTS) "
            + "VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE EMAIL_ADDRESS=VALUES(EMAIL_ADDRESS), "
            + "PASSWORD_HASH=VALUES(PASSWORD_HASH), PASSWORD_TYPE=VALUES(PASSWORD_TYPE),"
            + "SALT=VALUES(SALT),LOGIN_ATTEMPTS=VALUES(LOGIN_ATTEMPTS)";

    private static final String COUNT_MATCHING_PLAYER_PROFILES = "SELECT COUNT(*) FROM LOBBY_USER WHERE PLAYER_ID=?";

    private static final String FIND_BY_EMAIL_ADDRESS_SQL = "SELECT * FROM YAZINO_LOGIN WHERE EMAIL_ADDRESS=?";

    private static final String EXISTS_WITH_EMAIL_ADDRESS_SQL = "SELECT PLAYER_ID FROM YAZINO_LOGIN "
            + "WHERE EMAIL_ADDRESS=?";

    private static final String FIND_BY_PLAYER_ID_SQL = "SELECT * FROM YAZINO_LOGIN WHERE PLAYER_ID = ?";

    private static final String UPDATE_LOGIN_ATTEMPTS
            = "UPDATE YAZINO_LOGIN SET LOGIN_ATTEMPTS=LOGIN_ATTEMPTS+1 WHERE EMAIL_ADDRESS=?";

    private static final String RESET_LOGIN_ATTEMPTS
            = "UPDATE YAZINO_LOGIN SET LOGIN_ATTEMPTS=0 WHERE EMAIL_ADDRESS=?";

    private static final String DELETE_BY_PLAYER_ID = "DELETE FROM YAZINO_LOGIN WHERE PLAYER_ID = ?";

    private static final YazinoLoginRowMapper YAZINO_LOGIN_MAPPER = new YazinoLoginRowMapper();

    private final JdbcTemplate template;

    @Autowired
    public JDBCYazinoLoginDao(@Qualifier("jdbcTemplate") final JdbcTemplate template) {
        notNull(template, "template is null");
        this.template = template;
    }

    @Override
    public void save(final YazinoLogin login) {
        notNull(login, "login is null");
        notNull(login.getPlayerId(), "playerId is null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating login {}");
        }

        final int matches = template.queryForInt(COUNT_MATCHING_PLAYER_PROFILES, login.getPlayerId());
        if (matches != 1) {
            throw new IllegalArgumentException("Expected 1 PlayerProfile. Found " + matches
                    + " for player ID " + login.getPlayerId());
        }

        template.update(new PreparedStatementCreator() {
            @SuppressWarnings("UnusedAssignment")
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(INSERT_OR_UPDATE_SQL);
                int nextArgIndex = 1;
                st.setBigDecimal(nextArgIndex++, login.getPlayerId());
                st.setString(nextArgIndex++, login.getEmail());
                st.setString(nextArgIndex++, login.getPasswordHash());
                st.setString(nextArgIndex++, login.getPasswordType().toString());
                st.setBytes(nextArgIndex++, login.getSalt());
                st.setInt(nextArgIndex++, login.getLoginAttempts());
                return st;
            }
        });
    }

    @Override
    public void incrementLoginAttempts(final String emailAddress) {
        notNull(emailAddress, "emailAddress may not be null");

        template.update(UPDATE_LOGIN_ATTEMPTS, emailAddress);
    }

    @Override
    public void resetLoginAttempts(final String emailAddress) {
        notNull(emailAddress, "emailAddress may not be null");

        template.update(RESET_LOGIN_ATTEMPTS, emailAddress);
    }

    @Override
    public void deleteByPlayerId(BigDecimal playerId) {
        template.update(DELETE_BY_PLAYER_ID, playerId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean existsWithEmailAddress(final String emailAddress) {
        notNull(emailAddress, "Email Address may not be null");

        return template.query(EXISTS_WITH_EMAIL_ADDRESS_SQL,
                new ResultSetExtractor<Boolean>() {
                    @Override
                    public Boolean extractData(final ResultSet rs)
                            throws SQLException, DataAccessException {
                        return rs.next();
                    }
                }, emailAddress);
    }

    @SuppressWarnings("unchecked")
    @Override
    public YazinoLogin findByEmailAddress(final String emailAddress) {
        notNull(emailAddress, "Email Address may not be null");
        return getYazinoLogin(FIND_BY_EMAIL_ADDRESS_SQL, emailAddress);
    }


    @Override
    public YazinoLogin findByPlayerId(final BigDecimal playerId) {
        notNull(playerId, "Player ID may not be null");
        return getYazinoLogin(FIND_BY_PLAYER_ID_SQL, playerId);
    }

    private YazinoLogin getYazinoLogin(final String sql, final Object... args) {
        final List<YazinoLogin> userList = template.query(sql, YAZINO_LOGIN_MAPPER, args);
        if (userList != null && userList.size() > 0) {
            return userList.get(0);
        }

        return null;
    }

    @Override
    public Map<String, BigDecimal> findRegisteredEmailAddresses(final String... candidateEmailAddresses) {
        if (candidateEmailAddresses == null || candidateEmailAddresses.length == 0) {
            return Collections.emptyMap();
        }

        final Map<String, BigDecimal> matches = new HashMap<String, BigDecimal>();

        final StringBuilder sql =
                new StringBuilder("SELECT EMAIL_ADDRESS, PLAYER_ID FROM YAZINO_LOGIN WHERE EMAIL_ADDRESS IN (");
        sql.append(Joiner.on(",").join(Collections.nCopies(candidateEmailAddresses.length, "?")));
        sql.append(")");

        template.query(sql.toString(), candidateEmailAddresses, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet resultSet) throws SQLException {
                String email = resultSet.getString(1);
                BigDecimal playerId = resultSet.getBigDecimal(2);
                matches.put(email, playerId);
            }
        });

        return matches;
    }

    private static class YazinoLoginRowMapper implements RowMapper<YazinoLogin> {
        @Override
        public YazinoLogin mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new YazinoLogin(BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")),
                    rs.getString("EMAIL_ADDRESS"),
                    rs.getString("PASSWORD_HASH"),
                    PasswordType.valueOf(rs.getString("PASSWORD_TYPE")),
                    rs.getBytes("SALT"),
                    rs.getInt("LOGIN_ATTEMPTS"));
        }
    }
}
