package com.yazino.platform.persistence.tournament;

import com.yazino.platform.model.tournament.TrophyLeaderboardResult;
import com.yazino.platform.tournament.TrophyLeaderboardPlayerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Database persistence for {@link com.yazino.platform.model.tournament.TrophyLeaderboardResult}s.
 */
@Repository
public class JDBCTrophyLeaderboardResultDAO implements TrophyLeaderboardResultDao {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCTrophyLeaderboardResultDAO.class);

    private static final String VALUES_BLOCK = "(?,?,?,?,?,?,?,?)";
    private static final String INSERT_RESULT = "INSERT INTO LEADERBOARD_RESULT "
            + "(LEADERBOARD_ID,RESULT_TS,EXPIRY_TS,PLAYER_ID,LEADERBOARD_POSITION,"
            + "PLAYER_POINTS,PLAYER_PAYOUT,PLAYER_NAME) "
            + "VALUES ";

    private final JdbcTemplate template;

    @Autowired
    public JDBCTrophyLeaderboardResultDAO(final JdbcTemplate template) {
        notNull(template, "JDBC Template may not be null");
        this.template = template;
    }

    @Override
    public void save(final TrophyLeaderboardResult trophyLeaderboardResult) {
        notNull(trophyLeaderboardResult, "Trophy Leaderboard Result may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Saving trophy leaderboard result " + trophyLeaderboardResult);
        }

        if (trophyLeaderboardResult.getPlayerResults() == null
                || trophyLeaderboardResult.getPlayerResults().isEmpty()) {
            LOG.error("Results have no player results: " + trophyLeaderboardResult);
            return;
        }

        final StringBuilder queryBuilder = new StringBuilder(INSERT_RESULT);
        for (int i = 0; i < trophyLeaderboardResult.getPlayerResults().size(); ++i) {
            if (i > 0) {
                queryBuilder.append(",");
            }
            queryBuilder.append(VALUES_BLOCK);
        }

        template.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(queryBuilder.toString());

                int index = 1;
                for (final TrophyLeaderboardPlayerResult playerResult : trophyLeaderboardResult.getPlayerResults()) {
                    st.setBigDecimal(index++, trophyLeaderboardResult.getLeaderboardId());
                    st.setTimestamp(index++, new Timestamp(trophyLeaderboardResult.getResultTime().getMillis()));
                    st.setTimestamp(index++, new Timestamp(trophyLeaderboardResult.getExpiryTime().getMillis()));
                    st.setBigDecimal(index++, playerResult.getPlayerId());
                    st.setInt(index++, playerResult.getPosition());
                    st.setLong(index++, playerResult.getPoints());
                    st.setBigDecimal(index++, playerResult.getPayout());
                    st.setString(index++, playerResult.getPlayerName());
                }

                return st;
            }
        });

        if (LOG.isDebugEnabled()) {
            LOG.debug("Trophy leaderboard result saved for leaderboard" + trophyLeaderboardResult.getLeaderboardId());
        }
    }
}
