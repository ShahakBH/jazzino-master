package com.yazino.platform.persistence.tournament;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.tournament.RecurringTournamentDefinition;
import com.yazino.platform.model.tournament.TournamentVariationTemplateBuilder;
import com.yazino.platform.tournament.DayPeriod;
import com.yazino.platform.tournament.TournamentType;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link com.yazino.platform.persistence.tournament.JDBCRecurringTournamentDefinitionDAO} class.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCRecurringTournamentDefinitionDAOIntegrationTest {

    @Autowired(required = true)
    private JDBCRecurringTournamentDefinitionDAO dao;

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbc;

    private final long signupPeriod = 6L * DateTimeConstants.MILLIS_PER_HOUR;
    private final long frequency = 30L * DateTimeConstants.MILLIS_PER_MINUTE;
    private final DayPeriod exclusionPeriod = new DayPeriod("Tuesday@08:56-17:46");

    private DateTime signupTime = new DateTime();
    private TournamentVariationTemplate tournamentVariationTemplate;
    private BigInteger tournamentDefinitionID;
    private String tournamentName = "TEST_NAME";
    private String tournamentDescription = "TEST_DESCRIPTION";
    private String partnerID = "YAZINO";


    @Before
    @Transactional
    public void setup() {
        dao = new JDBCRecurringTournamentDefinitionDAO(jdbc);
        jdbc.execute("DELETE FROM RECURRING_TOURNAMENT_DEFINITION");
        signupTime = signupTime.withTime(signupTime.getHourOfDay(), signupTime.getMinuteOfHour(), signupTime.getSecondOfMinute(), 0);
        tournamentVariationTemplate = addTournamentVariationTemplate();
        tournamentDefinitionID = addRecurringTournament(tournamentVariationTemplate.getTournamentVariationTemplateId());
    }

    @After
    public void tearDown() {
        jdbc.execute("DELETE FROM RECURRING_TOURNAMENT_DEFINITION");
    }

    @Test
    @Transactional
    public void shouldReturnEmptyListWhenNoResults() throws Exception {
        jdbc.execute("DELETE FROM RECURRING_TOURNAMENT_DEFINITION");
        Collection<RecurringTournamentDefinition> definitions = toList(dao.iterateAll());
        assertTrue(definitions.isEmpty());
    }

    @Test
    @Transactional
    public void shouldRetrieveCorrectDefinitionID() throws Exception {
        Collection<RecurringTournamentDefinition> definitions = toList(dao.iterateAll());
        assertEquals(1, definitions.size());
        RecurringTournamentDefinition definition = definitions.iterator().next();
        assertEquals(tournamentDefinitionID, definition.getId());
    }

    @Test
    @Transactional
    public void shouldRetrieveCorrectInitialSignupTime() throws Exception {
        Collection<RecurringTournamentDefinition> definitions = toList(dao.iterateAll());
        assertEquals(1, definitions.size());
        RecurringTournamentDefinition definition = definitions.iterator().next();
        assertEquals(signupTime, definition.getInitialSignupTime());
    }

    @Test
    @Transactional
    public void shouldRetrieveCorrectSignupPeriod() throws Exception {
        Collection<RecurringTournamentDefinition> definitions = toList(dao.iterateAll());
        assertEquals(1, definitions.size());
        RecurringTournamentDefinition definition = definitions.iterator().next();
        assertEquals(signupPeriod, (long) definition.getSignupPeriod());
    }

    @Test
    @Transactional
    public void shouldRetrieveCorrectFrequency() throws Exception {
        Collection<RecurringTournamentDefinition> definitions = toList(dao.iterateAll());
        assertEquals(1, definitions.size());
        RecurringTournamentDefinition definition = definitions.iterator().next();
        assertEquals(frequency, (long) definition.getFrequency());
    }

    @Test
    @Transactional
    public void shouldRetrieveCorrectExclusionPeriods() throws Exception {
        Collection<RecurringTournamentDefinition> definitions = toList(dao.iterateAll());
        assertEquals(1, definitions.size());
        RecurringTournamentDefinition definition = definitions.iterator().next();
        DayPeriod[] exclusionPeriods = definition.getExclusionPeriods();
        assertEquals(1, exclusionPeriods.length);
        assertEquals(exclusionPeriod, exclusionPeriods[0]);
    }

    @Test
    @Transactional
    public void shouldRetrieveCorrectTournamentVariationTemplateID() throws Exception {
        Collection<RecurringTournamentDefinition> definitions = toList(dao.iterateAll());
        assertEquals(1, definitions.size());
        RecurringTournamentDefinition definition = definitions.iterator().next();
        assertEquals(tournamentVariationTemplate, definition.getTournamentVariationTemplate());
    }

    @Test
    @Transactional
    public void shouldRetrieveCorrectTournamentName() throws Exception {
        Collection<RecurringTournamentDefinition> definitions = toList(dao.iterateAll());
        assertEquals(1, definitions.size());
        RecurringTournamentDefinition definition = definitions.iterator().next();
        assertEquals(tournamentName, definition.getTournamentName());
    }

    @Test
    @Transactional
    public void shouldRetrieveCorrectTournamentDescription() throws Exception {
        Collection<RecurringTournamentDefinition> definitions = toList(dao.iterateAll());
        assertEquals(1, definitions.size());
        RecurringTournamentDefinition definition = definitions.iterator().next();
        assertEquals(tournamentDescription, definition.getTournamentDescription());
    }

    @Test
    @Transactional
    public void shouldRetrieveCorrectTournamentPartnerID() throws Exception {
        Collection<RecurringTournamentDefinition> definitions = toList(dao.iterateAll());
        assertEquals(1, definitions.size());
        RecurringTournamentDefinition definition = definitions.iterator().next();
        assertEquals(partnerID, definition.getPartnerId());
    }

    @Test
    @Transactional
    public void shouldRetrieveCorrectEnabledFlag() throws Exception {
        Collection<RecurringTournamentDefinition> definitions = toList(dao.iterateAll());
        assertEquals(1, definitions.size());
        RecurringTournamentDefinition definition = definitions.iterator().next();
        assertEquals(true, definition.isEnabled().booleanValue());
    }

    private <T> List<T> toList(final DataIterator<T> dataIterator) {
        final List<T> dataList = new ArrayList<T>();
        while (dataIterator.hasNext()) {
            dataList.add(dataIterator.next());
        }
        return dataList;
    }

    private BigInteger addRecurringTournament(final BigDecimal tournamentVariationTemplateID) {
        PreparedStatementCreator psc = new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement ps = con.prepareStatement("INSERT INTO RECURRING_TOURNAMENT_DEFINITION (INITIAL_SIGNUP_TIME, SIGNUP_PERIOD, FREQUENCY, TOURNAMENT_VARIATION_TEMPLATE_ID, EXCLUSION_PERIODS, TOURNAMENT_NAME, TOURNAMENT_DESCRIPTION, PARTNER_ID, ENABLED) VALUES (?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                ps.setTimestamp(1, new Timestamp(signupTime.getMillis()));
                ps.setLong(2, signupPeriod);
                ps.setLong(3, frequency);
                ps.setBigDecimal(4, tournamentVariationTemplateID);
                ps.setString(5, exclusionPeriod.toFormattedPeriod());
                ps.setString(6, tournamentName);
                ps.setString(7, tournamentDescription);
                ps.setString(8, partnerID);
                ps.setInt(9, 1);
                return ps;
            }
        };

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(psc, keyHolder);

        int id = keyHolder.getKey().intValue();
        return BigInteger.valueOf(id);
    }

    private TournamentVariationTemplate addTournamentVariationTemplate() {
        return new TournamentVariationTemplateBuilder(jdbc)
                .setTournamentType(TournamentType.PRESET)
                .setTemplateName("templ1")
                .setEntryFee(new BigDecimal("20.0000"))
                .setServiceFee(new BigDecimal("1.0000"))
                .setStartingChips(new BigDecimal("100.0000"))
                .setMinPlayers(1)
                .setMaxPlayers(10)
                .setGameType("BLACKJACK")
                .saveToDatabase();
    }
}