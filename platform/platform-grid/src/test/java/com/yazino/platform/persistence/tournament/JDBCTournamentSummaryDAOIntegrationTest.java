package com.yazino.platform.persistence.tournament;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.tournament.TournamentPlayerSummary;
import org.hamcrest.MatcherAssert;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCTournamentSummaryDAOIntegrationTest {

    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(4423542354354L);
    private static final Date FINISH_DATE = new DateTime(2008, 11, 16, 12, 33, 20, 0).toDate();
    private static final String TOURNAMENT_NAME = "test test baby";

    // this needs to use the interface to avoid autowiring errors from the proxies
    @Autowired(required = true)
    private TournamentSummaryDao underTest;

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private SequenceGenerator sequenceGenerator;

    @Before
    public void setUp() {
        final BigDecimal accountId = sequenceGenerator.next();
        jdbc.update("INSERT INTO ACCOUNT(ACCOUNT_ID,NAME) values (?,'testt1')", accountId);
        jdbc.update("INSERT INTO TOURNAMENT (TOURNAMENT_ID, TOURNAMENT_VARIATION_TEMPLATE_ID, TOURNAMENT_ACCOUNT_ID, "
                + "TOURNAMENT_POT_ACCOUNT_ID, TOURNAMENT_NAME, PARTNER_ID) VALUES (?,?,?,?,?,?)",
                TOURNAMENT_ID, BigDecimal.ONE, accountId, accountId, "Test tourney", "INTERNAL");
    }

    @Test
    @Transactional
    public void shouldInsertObjectAndSerialisePlayersCorrectly() {
        final TournamentSummary summary = buildSummary();

        underTest.save(summary);

        final Map query = jdbc.queryForMap("SELECT * FROM TOURNAMENT_SUMMARY WHERE TOURNAMENT_ID=?", TOURNAMENT_ID);

        assertThat((BigDecimal) query.get("TOURNAMENT_ID"), is(comparesEqualTo(TOURNAMENT_ID)));
        assertEquals(TOURNAMENT_NAME, query.get("TOURNAMENT_NAME"));
        assertEquals(FINISH_DATE, query.get("TOURNAMENT_FINISHED_TS"));
        assertEquals("1\t1\ttest 1\t50\tpicture1\n2\t2\ttest 2\t30\tpicture2\n3\t3\ttest 3\t20\tpicture3\n", query.get("TOURNAMENT_PLAYERS"));
    }

    @Test
    @Transactional
    public void shouldReadObjectAndDeserialisePlayersCorrectly() {
        final TournamentSummary summary = buildSummary();

        underTest.save(summary);

        final DataIterator<TournamentSummary> dataIterator = ((JDBCTournamentSummaryDAO) underTest).iterateAll();

        final List<TournamentSummary> summaries = new ArrayList<TournamentSummary>();
        while (dataIterator.hasNext()) {
            summaries.add(dataIterator.next());
        }
        assertEquals(1, summaries.size());
        assertEquals(summary, summaries.iterator().next());
    }

    @Test
    @Transactional
    public void deletingASummaryShouldRemoveItFromTheDatabase() {
        final TournamentSummary summary = buildSummary();
        underTest.save(summary);

        underTest.delete(summary.getTournamentId());

        assertThat(jdbc.queryForInt("SELECT COUNT(*) FROM TOURNAMENT_SUMMARY WHERE TOURNAMENT_ID=?", TOURNAMENT_ID),
                is(equalTo(0)));
    }

    private TournamentSummary buildSummary() {
        final TournamentSummary summary = new TournamentSummary();
        summary.setTournamentId(TOURNAMENT_ID);
        summary.setTournamentName(TOURNAMENT_NAME);
        summary.setGameType("BLACKJACK");
        summary.setFinishDateTime(FINISH_DATE);
        summary.addPlayer(new TournamentPlayerSummary(BigDecimal.valueOf(1), 1, "test 1", BigDecimal.valueOf(50), "picture1"));
        summary.addPlayer(new TournamentPlayerSummary(BigDecimal.valueOf(2), 2, "test 2", BigDecimal.valueOf(30), "picture2"));
        summary.addPlayer(new TournamentPlayerSummary(BigDecimal.valueOf(3), 3, "test 3", BigDecimal.valueOf(20), "picture3"));
        return summary;
    }
}
