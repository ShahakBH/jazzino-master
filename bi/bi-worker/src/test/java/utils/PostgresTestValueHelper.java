package utils;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.audit.message.SessionKey;
import com.yazino.platform.event.message.PlayerEvent;
import com.yazino.platform.event.message.PlayerProfileEvent;
import com.yazino.platform.event.message.PlayerReferrerEvent;
import com.yazino.platform.player.PlayerProfileStatus;
import org.joda.time.DateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import strata.server.worker.audit.persistence.PostgresSessionKeyDAO;
import strata.server.worker.event.persistence.PostgresPlayerDWDAO;
import strata.server.worker.event.persistence.PostgresPlayerProfileDWDAO;
import strata.server.worker.event.persistence.PostgresPlayerReferrerDWDAO;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;

public class PostgresTestValueHelper {

    public static void createAPlayer(JdbcTemplate jdbc, BigDecimal... playerIds) {
        for (BigDecimal playerId : playerIds) {
            insertPlayer(jdbc, playerId);
        }
    }

    public static void createAPlayer(JdbcTemplate jdbc, int... playerIds) {
        for (int playerId : playerIds) {
            insertPlayer(jdbc, new BigDecimal(playerId));
        }
    }

    private static void insertPlayer(final JdbcTemplate jdbc, final BigDecimal playerId) {
        jdbc.update("DELETE FROM AUDIT_COMMAND WHERE player_id=?", playerId);
        jdbc.update("DELETE FROM lobby_user WHERE player_id=?", playerId);
        jdbc.update("INSERT INTO lobby_user (player_id, provider_name, rpx_provider, blocked) VALUES (?,?,?,?)",
                playerId, "your mum", "yr_mum", false);
    }

    public static void createAnAccount(final JdbcTemplate jdbc, final BigDecimal accountId) {
        jdbc.update("DELETE FROM account WHERE account_id=?", accountId);
        jdbc.update("INSERT INTO account (account_id, balance) VALUES (?,?)", accountId, 10000);
    }

    public static void createPlayerProfileAndRef(final PostgresPlayerProfileDWDAO postgresPlayerProfileDWDAO,
                                                 final PostgresPlayerDWDAO postgresPlayerDWDAO,
                                                 final PostgresPlayerReferrerDWDAO postgresPlayerReferrerDWDAO,
                                                 BigDecimal playerId,
                                                 DateTime created,
                                                 BigDecimal accountId) {
        postgresPlayerProfileDWDAO.saveAll(newArrayList(
                new PlayerProfileEvent(
                        playerId,
                        created,
                        "displayNam",
                        "realname",
                        "firstname",
                        "picture",
                        "email",
                        "UK",
                        "externalID",
                        "verificationId",
                        "providerName",
                        PlayerProfileStatus.ACTIVE,
                        Partner.YAZINO,
                        new DateTime(),
                        "m",
                        "referrer",
                        "127.0.0.1",
                        true,
                        "LastName",
                        "G")));

        postgresPlayerDWDAO.saveAll(newArrayList(new PlayerEvent(playerId, created, accountId, Collections.<String>emptySet())));
        if (postgresPlayerReferrerDWDAO != null) {
            postgresPlayerReferrerDWDAO.saveAll(newArrayList(new PlayerReferrerEvent(playerId, "referrer", "WEB", "gametype")));
        }
    }

    public static void createSession(final PostgresSessionKeyDAO postgresSessionKeyDAO,
                                        final JdbcTemplate jdbcTemplate,
                                        final BigDecimal playerId,
                                        final DateTime auditTs,
                                        final Platform platform,
                                        final BigDecimal accountId,
                                        final BigDecimal sessionId) {
        final String sessionKey = "SESSIONKEY" + auditTs.getMillis();
        postgresSessionKeyDAO.saveAll(asList(new SessionKey(sessionId,
                accountId,
                playerId,
                sessionKey,
                "127.0.0.1",
                "referrer",
                platform.name(),
                "www.loginurl.com", new HashMap<String, Object>())));

        jdbcTemplate.update("update account_session set start_ts = ? where session_key =?", new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps) throws SQLException {
                ps.setTimestamp(1, new Timestamp(auditTs.getMillis()));
                ps.setString(2, sessionKey);
            }
        });
    }
}
