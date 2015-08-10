package com.yazino.platform.persistence.community;

import com.yazino.platform.community.Trophy;
import org.junit.After;
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
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCTrophyDAOIntegrationTest {
    private static final BigDecimal TROPHY_ID = BigDecimal.valueOf(99999998L);
    private static final String TROPHY_IMAGE = "trophyImage";
    private static final String TROPHY_NAME = "trophyTest-t";
    private static final String GAME_TYPE = "BLACKJACK";
    private static final String MESSAGE = "A test message";
    private static final String SHORT_DESCRIPTION = "A test short description";
    private static final String MESSAGE_CABINET = "A test message cabinet";

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = true)
    private TrophyDAO trophyDAO;

    private Trophy trophy;


    @Before
    @Transactional
    public void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM TROPHY WHERE TROPHY_NAME = ?", "TEST");

        trophy = new Trophy(TROPHY_ID, TROPHY_NAME, GAME_TYPE, TROPHY_IMAGE);
        trophy.setMessage(MESSAGE);
        trophy.setShortDescription(SHORT_DESCRIPTION);
        trophy.setMessageCabinet(MESSAGE_CABINET);
    }

    @After
    @Transactional
    public void cleanUp() {
        jdbcTemplate.update("DELETE FROM TROPHY WHERE TROPHY_NAME LIKE ?", TROPHY_NAME + "%");
        jdbcTemplate.update("DELETE FROM PLAYER WHERE NAME LIKE ?", "trophyTest-p%");
        jdbcTemplate.update("DELETE FROM ACCOUNT WHERE NAME LIKE ?", "trophyTest-a");
    }

    @Test
    @Transactional
    public void saveAddsNonExistentTrophyToSpace() {
        trophyDAO.save(trophy);

        final Map persistedTrophy = jdbcTemplate.queryForMap("SELECT * FROM TROPHY WHERE TROPHY_ID=?", TROPHY_ID);

        assertThat(persistedTrophy, is(not(nullValue())));
        assertThat((Integer) persistedTrophy.get("TROPHY_ID"), is(equalTo(TROPHY_ID.intValue())));
        assertThat((String) persistedTrophy.get("TROPHY_NAME"), is(equalTo(TROPHY_NAME)));
        assertThat((String) persistedTrophy.get("TROPHY_IMAGE"), is(equalTo(TROPHY_IMAGE)));
        assertThat((String) persistedTrophy.get("GAME_TYPE"), is(equalTo(GAME_TYPE)));
        assertThat((String) persistedTrophy.get("MESSAGE"), is(equalTo(MESSAGE)));
        assertThat((String) persistedTrophy.get("SHORT_DESCRIPTION"), is(equalTo(SHORT_DESCRIPTION)));
        assertThat((String) persistedTrophy.get("MESSAGE_CABINET"), is(equalTo(MESSAGE_CABINET)));
    }

    @Test
    @Transactional
    public void updateModifiesExistingTrophy() {
        trophyDAO.save(trophy);

        trophy.setName("TEST");
        trophy.setMessage("A Different Test Message");
        trophy.setShortDescription("A Different Test Short Description");
        trophy.setMessageCabinet("A Different Test Message cabinet");

        trophyDAO.save(trophy);

        final Map persistedTrophy = jdbcTemplate.queryForMap("SELECT * FROM TROPHY WHERE TROPHY_ID=?", TROPHY_ID);

        assertThat(persistedTrophy, is(not(nullValue())));
        assertThat((Integer) persistedTrophy.get("TROPHY_ID"), is(equalTo(TROPHY_ID.intValue())));
        assertThat((String) persistedTrophy.get("TROPHY_NAME"), is(equalTo("TEST")));
        assertThat((String) persistedTrophy.get("MESSAGE"), is(equalTo("A Different Test Message")));
        assertThat((String) persistedTrophy.get("SHORT_DESCRIPTION"), is(equalTo("A Different Test Short Description")));
        assertThat((String) persistedTrophy.get("MESSAGE_CABINET"), is(equalTo("A Different Test Message cabinet")));
    }

}
