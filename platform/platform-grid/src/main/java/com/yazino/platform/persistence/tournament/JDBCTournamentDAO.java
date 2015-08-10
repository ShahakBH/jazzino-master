package com.yazino.platform.persistence.tournament;

import com.gigaspaces.datasource.DataIterator;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.persistence.DataIterable;
import com.yazino.platform.persistence.ResultSetIterator;
import com.yazino.platform.tournament.TournamentStatus;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Database persistence for {@link Tournament}s.
 */
@Repository("tournamentDao")
public class JDBCTournamentDAO implements TournamentDao, DataIterable<Tournament> {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCTournamentDAO.class);

    private static final String INSERT_OR_UPDATE_TOURNAMENT = "INSERT INTO TOURNAMENT "
            + "(TOURNAMENT_ID,POT,TOURNAMENT_VARIATION_TEMPLATE_ID, "
            + "TOURNAMENT_START_TS,TOURNAMENT_SIGNUP_START_TS, TOURNAMENT_SIGNUP_END_TS, TOURNAMENT_STATUS, "
            + "TOURNAMENT_NAME,NEXT_EVENT_TS,PARTNER_ID,TOURNAMENT_DESCRIPTION,"
            + "TOURNAMENT_CURRENT_ROUND, SETTLED_PRIZE_POT)"
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE "
            + "POT=VALUES(POT),TOURNAMENT_VARIATION_TEMPLATE_ID=VALUES(TOURNAMENT_VARIATION_TEMPLATE_ID), "
            + "TOURNAMENT_START_TS=VALUES(TOURNAMENT_START_TS),"
            + "TOURNAMENT_SIGNUP_START_TS=VALUES(TOURNAMENT_SIGNUP_START_TS),"
            + "TOURNAMENT_SIGNUP_END_TS=VALUES(TOURNAMENT_SIGNUP_END_TS), "
            + "TOURNAMENT_STATUS=VALUES(TOURNAMENT_STATUS), "
            + "TOURNAMENT_NAME=VALUES(TOURNAMENT_NAME),NEXT_EVENT_TS=VALUES(NEXT_EVENT_TS),"
            + "PARTNER_ID=VALUES(PARTNER_ID),"
            + "TOURNAMENT_DESCRIPTION=VALUES(TOURNAMENT_DESCRIPTION),"
            + "TOURNAMENT_CURRENT_ROUND=VALUES(TOURNAMENT_CURRENT_ROUND),"
            + "SETTLED_PRIZE_POT=VALUES(SETTLED_PRIZE_POT)";

    private static final String SELECT_TOURNAMENT = "SELECT * FROM TOURNAMENT T,TOURNAMENT_VARIATION_TEMPLATE TVT "
            + "WHERE T.TOURNAMENT_STATUS NOT IN (?,?)"
            + " AND T.TOURNAMENT_VARIATION_TEMPLATE_ID = TVT.TOURNAMENT_VARIATION_TEMPLATE_ID";

    private static final String INSERT_TOURNAMENT_TABLE
            = "INSERT IGNORE INTO TOURNAMENT_TABLE(TOURNAMENT_ID, TABLE_ID) VALUES (?, ?)";

    private static final String SELECT_TOURNAMENT_TABLE = "SELECT TABLE_ID FROM TOURNAMENT_TABLE WHERE TOURNAMENT_ID=?";

    private final JdbcTemplate template;
    private final TournamentPlayerDao tournamentPlayerDao;
    private final RowMapper tournamentVariationTemplateRowMapper;
    private final RowMapper<Tournament> tournamentRowMapper = new TournamentRowMapper();

    @Autowired
    public JDBCTournamentDAO(final JdbcTemplate template,
                             final TournamentPlayerDao tournamentPlayerDao) {
        notNull(template, "JDBC Template may not be null");
        notNull(tournamentPlayerDao, "Tournament Player DAO may not be null");
        this.template = template;
        this.tournamentPlayerDao = tournamentPlayerDao;
        this.tournamentVariationTemplateRowMapper = new TournamentVariationTemplateRowMapper(template);
    }

    public void save(final Tournament tournament) {
        notNull(tournament, "Tournament may not be null");
        notNull(tournament.getTournamentId(), "Tournament ID may not be null");
        notNull(tournament.getTournamentVariationTemplate(),
                "Tournament Variation Template may not be null");
        notNull(tournament.getSignupStartTimeStamp(), "Tournament Signup Start Time Stamp may not be null");
        notNull(tournament.getTournamentStatus(), "Tournament Status may not be null");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating tournament " + tournament);
        }
        template.update(new PreparedStatementCreator() {
            @SuppressWarnings("UnusedAssignment")
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(
                        INSERT_OR_UPDATE_TOURNAMENT, Statement.RETURN_GENERATED_KEYS);
                int argIndex = 1;
                st.setBigDecimal(argIndex++, tournament.getTournamentId());
                st.setBigDecimal(argIndex++, tournament.getPot());
                st.setBigDecimal(argIndex++,
                        tournament.getTournamentVariationTemplate().getTournamentVariationTemplateId());
                if (tournament.getStartTimeStamp() != null) {
                    st.setTimestamp(argIndex++, new Timestamp(tournament.getStartTimeStamp().getMillis()));
                } else {
                    st.setNull(argIndex++, Types.TIMESTAMP);
                }

                st.setTimestamp(argIndex++, new Timestamp(tournament.getSignupStartTimeStamp().getMillis()));
                if (tournament.getSignupEndTimeStamp() != null) {
                    st.setTimestamp(argIndex++, new Timestamp(tournament.getSignupEndTimeStamp().getMillis()));
                } else {
                    st.setNull(argIndex++, Types.TIMESTAMP);
                }
                st.setString(argIndex++, tournament.getTournamentStatus().getId());
                st.setString(argIndex++, tournament.getName());
                if (tournament.getNextEvent() != null) {
                    st.setTimestamp(argIndex++, new Timestamp(tournament.getNextEvent()));
                } else {
                    st.setNull(argIndex++, Types.TIMESTAMP);
                }
                st.setString(argIndex++, tournament.getPartnerId());
                st.setString(argIndex++, tournament.getDescription());

                if (tournament.getCurrentRoundIndex() != null) {
                    st.setInt(argIndex++, tournament.getCurrentRoundIndex());
                } else {
                    st.setNull(argIndex++, Types.INTEGER);
                }
                st.setBigDecimal(argIndex++, tournament.getSettledPrizePot());

                return st;
            }
        });

        reconcileTournamentPlayers(tournament);

        if (tournament.getTables() != null) {
            final int tournamentTablesSize = tournament.getTables().size();
            final BigDecimal[] tournamentTables = tournament.getTables().toArray(new BigDecimal[tournamentTablesSize]);
            /*
               We are not deleting the existing tournament tables because if the
               tournament culls any existing tables as
               the rounds progress we will loose tournament->table associations.
            */
            template.batchUpdate(INSERT_TOURNAMENT_TABLE, new BatchPreparedStatementSetter() {
                public void setValues(final PreparedStatement ps,
                                      final int i) throws SQLException {
                    ps.setBigDecimal(1, tournament.getTournamentId());
                    ps.setBigDecimal(2, tournamentTables[i]);
                }

                @Override
                public int getBatchSize() {
                    return tournamentTablesSize;
                }
            });
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Tournament created with ID %s: %s", tournament.getTournamentId(), tournament));
        }
    }

    private void reconcileTournamentPlayers(final Tournament tournament) {
        final Set<TournamentPlayer> persistedPlayers
                = tournamentPlayerDao.findByTournamentId(tournament.getTournamentId());
        final Set<TournamentPlayer> currentPlayers = new HashSet<TournamentPlayer>(tournament.tournamentPlayers());
        for (TournamentPlayer tournamentPlayer : persistedPlayers) {
            if (!Iterables.tryFind(currentPlayers, new FindByPlayerId(tournamentPlayer.getPlayerId())).isPresent()) {
                tournamentPlayerDao.remove(tournament.getTournamentId(), tournamentPlayer);
            }
        }
        for (TournamentPlayer tournamentPlayer : currentPlayers) {
            tournamentPlayerDao.save(tournament.getTournamentId(), tournamentPlayer);
        }
    }

    @Override
    public DataIterator<Tournament> iterateAll() {
        return new ResultSetIterator<Tournament>(template, new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                final PreparedStatement stmt = con.prepareStatement(SELECT_TOURNAMENT);
                stmt.setString(1, TournamentStatus.CLOSED.getId());
                stmt.setString(2, TournamentStatus.ERROR.getId());
                return stmt;
            }
        }, tournamentRowMapper);
    }

    @SuppressWarnings("unchecked")
    public List<Tournament> findNonClosedTournaments() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding non-closed tournaments");
        }

        final List<Tournament> results = template.query(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(SELECT_TOURNAMENT);
                st.setString(1, TournamentStatus.CLOSED.getId());
                st.setString(2, TournamentStatus.ERROR.getId());
                return st;
            }
        }, tournamentRowMapper);

        if (results != null) {
            return new ArrayList<Tournament>(results);
        }

        return new ArrayList<Tournament>();
    }

    private DateTime readNullableDateTime(final Timestamp value) {
        if (value == null) {
            return null;
        }
        return new DateTime(value.getTime());
    }

    private Long readNullableMillis(final Timestamp value) {
        if (value == null) {
            return null;
        }
        return value.getTime();
    }

    private class TournamentRowMapper implements RowMapper<Tournament> {
        public Tournament mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Mapping row " + rowNum);
            }
            final BigDecimal tournamentId = BigDecimals.strip(rs.getBigDecimal("TOURNAMENT_ID"));
            final Set<TournamentPlayer> players = tournamentPlayerDao.findByTournamentId(tournamentId);

            final Tournament tournament = new Tournament(tournamentId, players);
            tournament.setPot(rs.getBigDecimal("POT"));
            tournament.setName(rs.getString("TOURNAMENT_NAME"));
            tournament.setSignupStartTimeStamp(new DateTime(rs.getTimestamp("TOURNAMENT_SIGNUP_START_TS").getTime()));
            tournament.setSignupEndTimeStamp(readNullableDateTime(rs.getTimestamp("TOURNAMENT_SIGNUP_END_TS")));
            tournament.setStartTimeStamp(readNullableDateTime(rs.getTimestamp("TOURNAMENT_START_TS")));
            tournament.setTournamentStatus(TournamentStatus.getById(rs.getString("TOURNAMENT_STATUS")));
            tournament.setNextEvent(readNullableMillis(rs.getTimestamp("NEXT_EVENT_TS")));
            tournament.setPartnerId(rs.getString("PARTNER_ID"));
            final List<BigDecimal> tableIds = template.queryForList(
                    SELECT_TOURNAMENT_TABLE, BigDecimal.class, tournamentId);
            tournament.setTables(tableIds);
            tournament.setTournamentVariationTemplate(
                    (TournamentVariationTemplate) tournamentVariationTemplateRowMapper.mapRow(rs, rowNum));
            tournament.setDescription(rs.getString("TOURNAMENT_DESCRIPTION"));
            tournament.setSettledPrizePot(rs.getBigDecimal("SETTLED_PRIZE_POT"));

            tournament.setCurrentRoundIndex(rs.getInt("TOURNAMENT_CURRENT_ROUND"));
            if (rs.wasNull()) {
                tournament.setCurrentRoundIndex(null);
            }

            return tournament;
        }
    }


    private static class FindByPlayerId implements Predicate<TournamentPlayer> {
        private BigDecimal playerId;

        FindByPlayerId(final BigDecimal playerId) {
            notNull(playerId, "Player ID may not be null");
            this.playerId = playerId;
        }

        @Override
        public boolean apply(final TournamentPlayer candidatePlayer) {
            return playerId.compareTo(candidatePlayer.getPlayerId()) == 0;
        }
    }
}
