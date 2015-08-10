package com.yazino.platform.persistence.statistic;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.yazino.platform.model.statistic.Achievement;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
public class JDBCAchievementDAOIntegrationTest {
    int baseCount;
    private final String testId = UUID.randomUUID().toString();
    private final Achievement achievement1 = new Achievement("achieve1_" + testId, 1, "achieve1", "You've earned achieve 1",
            "Yay! Achieve 1", "how to achieve 1", "Achieved", "blackjack", "Play", "blackjack", "BLACKJACK", set("anEvent", "anotherEvent"), "anAccumulator", null, true);
    private final Achievement achievement2 = new Achievement("achieve2_" + testId, 2, "achieve2", "You've earned achieve 2",
            "Yay! Achieve 2", "how to achieve 2", "Achieved", "texasHoldem", "Play", "texasHoldem", "TEXAS_HOLDEM", set("anEvent"), "anAccumulator", "params", false);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AchievementDAO achievementDAO;

    @Before
    public void setUp() {
        clean();
        baseCount = jdbcTemplate.queryForInt("SELECT count(*) FROM ACHIEVEMENT");
        jdbcTemplate.update("INSERT INTO ACHIEVEMENT (ACHIEVEMENT_ID, ACHIEVEMENT_TITLE, ACHIEVEMENT_MESSAGE, "
                + "ACHIEVEMENT_SHORT_DESCRIPTION, POSTED_ACHIEVEMENT_TITLE_TEXT, POSTED_ACHIEVEMENT_TITLE_LINK, POSTED_ACHIEVEMENT_ACTION_NAME, POSTED_ACHIEVEMENT_ACTION_LINK, " +
                "GAME_TYPE, EVENT, ACCUMULATOR, ACCUMULATOR_PARAMS,ACHIEVEMENT_LEVEL,ACHIEVEMENT_HOW_TO_GET, RECURRING) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?),(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                achievement1.getId(), achievement1.getTitle(), achievement1.getMessage(), achievement1.getShortDescription(),
                achievement1.getPostedAchievementTitleText(), achievement1.getPostedAchievementTitleLink(), achievement1.getPostedAchievementActionText(),
                achievement1.getPostedAchievementActionLink(), achievement1.getGameType(), Joiner.on(",").join(achievement1.getEvents()), achievement1.getAccumulator(), achievement1.getAccumulatorParameters(),
                achievement1.getLevel(), achievement1.getHowToGet(), achievement1.getRecurring(),
                achievement2.getId(), achievement2.getTitle(), achievement2.getMessage(), achievement2.getShortDescription(),
                achievement2.getPostedAchievementTitleText(), achievement2.getPostedAchievementTitleLink(), achievement2.getPostedAchievementActionText(),
                achievement2.getPostedAchievementActionLink(), achievement2.getGameType(), Joiner.on(",").join(achievement2.getEvents()), achievement2.getAccumulator(), achievement2.getAccumulatorParameters(),
                achievement2.getLevel(), achievement2.getHowToGet(), achievement2.getRecurring());
    }

    @After
    public void tearDown() {
        clean();
    }

    @Test
    @Transactional
    public void findAllReturnsAllAchievements() {
        final Collection<Achievement> match = achievementDAO.findAll();

        assertThat(match, is(not(nullValue())));
        assertThat(match.size(), is(equalTo(baseCount + 2)));
        assertThat(match, hasItem(achievement1));
        assertThat(match, hasItem(achievement2));
    }

    private void clean() {
        jdbcTemplate.update("DELETE FROM ACHIEVEMENT WHERE ACHIEVEMENT_ID LIKE ?", "%" + testId);
    }

    private <T> Set<T> set(final T... items) {
        return new HashSet<T>(Arrays.asList(items));
    }

}
