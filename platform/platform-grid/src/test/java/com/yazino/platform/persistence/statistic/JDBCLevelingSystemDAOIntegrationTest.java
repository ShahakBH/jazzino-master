package com.yazino.platform.persistence.statistic;

import com.yazino.platform.model.statistic.ExperienceFactor;
import com.yazino.platform.model.statistic.LevelDefinition;
import com.yazino.platform.model.statistic.LevelingSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
public class JDBCLevelingSystemDAOIntegrationTest {

    private int baseCount;
    private LevelingSystem levelSystem1;
    private LevelingSystem levelSystem2;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LevelingSystemDAO levelingSystemDAO;

    @Before
    public void setUp() {
        levelSystem1 = new LevelingSystem("TEXAS_HOLDEM", newHashSet(
                asList(new ExperienceFactor("EVENT_1", BigDecimal.TEN), new ExperienceFactor("EVENT_2", BigDecimal.TEN))),
                asList(new LevelDefinition(1, BigDecimal.ZERO, BigDecimal.valueOf(30), BigDecimal.valueOf(1000)),
                        new LevelDefinition(2, BigDecimal.valueOf(30), BigDecimal.valueOf(33), BigDecimal.valueOf(2000))));
        levelSystem2 = new LevelingSystem("BLACKJACK", newHashSet(
                asList(new ExperienceFactor("EVENT_11", BigDecimal.ONE), new ExperienceFactor("EVENT_22", BigDecimal.ONE))),
                asList(new LevelDefinition(1, BigDecimal.ZERO, BigDecimal.valueOf(25), BigDecimal.ONE),
                        new LevelDefinition(2, BigDecimal.valueOf(25), BigDecimal.valueOf(50), BigDecimal.valueOf(2))));
        jdbcTemplate.execute("DELETE FROM LEVEL_SYSTEM");
        baseCount = jdbcTemplate.queryForInt("SELECT count(*) FROM LEVEL_SYSTEM");
        jdbcTemplate.update("INSERT INTO LEVEL_SYSTEM (GAME_TYPE,EXPERIENCE_FACTORS, LEVEL_DEFINITIONS) VALUES (?,?,?),(?,?,?)",
                levelSystem1.getGameType(), "EVENT_1\t10\nEVENT_2\t10\n", "30\t1000\n33\t2000\n",
                levelSystem2.getGameType(), "EVENT_11\t1\nEVENT_22\t1\n", "25\t1\n50\t2\n");
    }

    @Test
    @Transactional
    public void findAllReturnsAllLevelSystems() {
        final Collection<LevelingSystem> match = levelingSystemDAO.findAll();

        assertThat(match, is(not(nullValue())));
        assertThat(match.size(), is(equalTo(baseCount + 2)));
        assertThat(match, hasItem(levelSystem1));
        assertThat(match, hasItem(levelSystem2));
    }
}
