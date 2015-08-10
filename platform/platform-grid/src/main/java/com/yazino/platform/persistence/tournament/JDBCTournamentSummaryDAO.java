package com.yazino.platform.persistence.tournament;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.persistence.DataIterable;
import com.yazino.platform.persistence.ResultSetIterator;
import com.yazino.platform.tournament.TournamentPlayerSummary;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Date;
import java.util.StringTokenizer;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("tournamentSummaryDao")
public class JDBCTournamentSummaryDAO implements TournamentSummaryDao, DataIterable<TournamentSummary> {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCTournamentSummaryDAO.class);

    private static final String SAVE_SQL = "INSERT INTO TOURNAMENT_SUMMARY "
            + "(TOURNAMENT_ID,TOURNAMENT_NAME,TOURNAMENT_FINISHED_TS,TOURNAMENT_PLAYERS) VALUES (?,?,?,?)";
    private static final String SELECT_SQL = "SELECT game_type, ts.*  FROM TOURNAMENT_SUMMARY ts, "
            + "(SELECT game_type, max(ts.TOURNAMENT_ID) TOURNAMENT_ID "
            + "FROM TOURNAMENT_SUMMARY ts, TOURNAMENT t, TOURNAMENT_VARIATION_TEMPLATE tv "
            + "WHERE ts.tournament_id = t.tournament_id "
            + "AND t.tournament_variation_template_id = tv.tournament_variation_template_id "
            + "GROUP BY game_type) x WHERE ts.tournament_id = x.tournament_id";
    private static final int PICTURE_URL_INDEX = 4;
    private static final int PLAYER_ID_INDEX = 0;
    private static final int LEADERBOARD_POSITION_INDEX = 1;
    private static final int NAME_INDEX = 2;
    private static final int PRIZE_INDEX = 3;
    private static final String DELETE_SQL = "DELETE FROM TOURNAMENT_SUMMARY WHERE TOURNAMENT_ID=?";

    private final TournamentSummaryRowMapper summaryRowMapper = new TournamentSummaryRowMapper();
    private final JdbcTemplate template;
    private static final String COLUMN_DELIMITER = "\t";
    private static final String ROW_DELIMITER = "\n";

    @Autowired
    public JDBCTournamentSummaryDAO(final JdbcTemplate template) {
        notNull(template, "JDBC Template may not be null");
        this.template = template;
    }

    @Override
    public void save(final TournamentSummary summary) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering save (" + summary + ")");
        }

        notNull(summary, "Tournament may not be null");

        template.update(new PreparedStatementCreator() {
            @SuppressWarnings("UnusedAssignment")
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(SAVE_SQL);
                int argIndex = 1;

                st.setBigDecimal(argIndex++, summary.getTournamentId());
                st.setString(argIndex++, summary.getTournamentName());
                st.setTimestamp(argIndex++, getTimestamp(summary.getFinishDateTime()));
                st.setString(argIndex++, serialisePlayers(summary));

                return st;
            }
        });
    }

    @Override
    public void delete(final BigDecimal tournamentId) {
        notNull(tournamentId, "tournamentId may not be null");

        template.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection connection) throws SQLException {
                final PreparedStatement statement = connection.prepareStatement(DELETE_SQL);
                statement.setBigDecimal(1, tournamentId);
                return statement;
            }
        });
    }

    private Timestamp getTimestamp(final Date date) {
        if (date == null) {
            return null;
        }
        return new Timestamp(date.getTime());
    }

    private String serialisePlayers(final TournamentSummary summary) {
        assert summary != null : "Summary may not be null";

        final StringBuilder builder = new StringBuilder();

        for (final TournamentPlayerSummary player : summary.playerSummaries()) {
            builder.append(player.getId()).append(COLUMN_DELIMITER);
            builder.append(player.getLeaderboardPosition()).append(COLUMN_DELIMITER);
            builder.append(player.getName().replace('\t', ' ')).append(COLUMN_DELIMITER);
            builder.append(player.getPrize()).append(COLUMN_DELIMITER);
            builder.append(emptyIfNull(player.getPictureUrl()));
            builder.append(ROW_DELIMITER);
        }

        return builder.toString();
    }

    private String emptyIfNull(final String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private void deserialisePlayers(final String serialisedPlayers, final TournamentSummary summary) {
        assert summary != null : "Summary may not be null";

        if (serialisedPlayers == null) {
            return;
        }

        final StringTokenizer recordSplitter = new StringTokenizer(serialisedPlayers, ROW_DELIMITER, true);

        boolean lastRecordWasDelimiter = true;
        while (recordSplitter.hasMoreTokens()) {
            final String record = recordSplitter.nextToken();

            // as string tokeniser strips out empty fields we detect them using two delimiters in a row
            if (record.equals("\n")) {
                if (!lastRecordWasDelimiter) {
                    lastRecordWasDelimiter = true;
                    continue;
                }
            }
            lastRecordWasDelimiter = false;
            if (record.trim().length() > 0) {
                final String[] playerFields = record.split(COLUMN_DELIMITER);
                if (playerFields.length < PICTURE_URL_INDEX
                        || playerFields.length > (PICTURE_URL_INDEX + 1)) {
                    throw new IllegalStateException("Incorrectly formatted player representation: " + record);
                }

                String pictureUrl = null;
                if (playerFields.length == (PICTURE_URL_INDEX + 1)
                        && StringUtils.isNotBlank(playerFields[PICTURE_URL_INDEX])) {
                    pictureUrl = playerFields[PICTURE_URL_INDEX];
                }

                final TournamentPlayerSummary playerSummary = new TournamentPlayerSummary(
                        new BigDecimal(playerFields[PLAYER_ID_INDEX]),
                        Integer.valueOf(playerFields[LEADERBOARD_POSITION_INDEX]),
                        playerFields[NAME_INDEX],
                        new BigDecimal(playerFields[PRIZE_INDEX]),
                        pictureUrl);
                summary.addPlayer(playerSummary);
            }
        }
    }

    @Override
    public DataIterator<TournamentSummary> iterateAll() {
        return new ResultSetIterator<TournamentSummary>(template, new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                return con.prepareStatement(SELECT_SQL);
            }
        }, summaryRowMapper);
    }

    private class TournamentSummaryRowMapper implements RowMapper<TournamentSummary> {
        @Override
        public TournamentSummary mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final TournamentSummary summary = new TournamentSummary();

            summary.setTournamentId(BigDecimals.strip(rs.getBigDecimal("TOURNAMENT_ID")));
            summary.setTournamentName(rs.getString("TOURNAMENT_NAME"));
            summary.setFinishDateTime(rs.getTimestamp("TOURNAMENT_FINISHED_TS"));
            summary.setGameType(rs.getString("GAME_TYPE"));
            deserialisePlayers(rs.getString("TOURNAMENT_PLAYERS"), summary);

            return summary;
        }
    }
}
