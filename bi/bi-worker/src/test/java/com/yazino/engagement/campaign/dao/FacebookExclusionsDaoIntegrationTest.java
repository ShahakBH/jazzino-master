package com.yazino.engagement.campaign.dao;

import com.yazino.platform.audit.message.SessionKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.campaign.dao.FacebookExclusionsDao.BOUNCED;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static utils.PlayerBuilder.*;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@TransactionConfiguration
public class FacebookExclusionsDaoIntegrationTest {


    @Autowired
    private FacebookExclusionsDao facebookExclusionsDao;

    @Autowired
    @Qualifier("externalDwNamedJdbcTemplate")
    private NamedParameterJdbcTemplate dwJdbcTemplate;

    @Before
    public void setUp() throws Exception {
        dwJdbcTemplate.update("delete from FACEBOOK_EXCLUSIONS where PLAYER_ID=:p",withP(ANDY));
        dwJdbcTemplate.update("delete from FACEBOOK_EXCLUSIONS where PLAYER_ID=:p",withP(BOB));
        dwJdbcTemplate.update("delete from FACEBOOK_EXCLUSIONS where PLAYER_ID=:p",withP(CHAZ));
        initialise(dwJdbcTemplate);
        createPlayer(ANDY).storeIn(dwJdbcTemplate);
        createPlayer(BOB).storeIn(dwJdbcTemplate);
        createPlayer(CHAZ).storeIn(dwJdbcTemplate);

    }

    @Test
    public void resetFacebookExclusionsShouldClearExclusionForThatPlayer(){
             dwJdbcTemplate.update("INSERT INTO FACEBOOK_EXCLUSIONS (PLAYER_ID, GAME_TYPE, REASON) VALUES (:p,'SLOTS','BOUNCED')", withP(1));
             dwJdbcTemplate.update("INSERT INTO FACEBOOK_EXCLUSIONS (PLAYER_ID, GAME_TYPE, REASON) VALUES (:p,'BONGO','DENIED')", withP(2));
             dwJdbcTemplate.update("INSERT INTO FACEBOOK_EXCLUSIONS (PLAYER_ID, GAME_TYPE, REASON) VALUES (:p,'SLOTS','BOUNCED')", withP(2));
             dwJdbcTemplate.update("INSERT INTO FACEBOOK_EXCLUSIONS (PLAYER_ID, GAME_TYPE, REASON) VALUES (:p,'BONGO','BOUNCED')", withP(3));
             dwJdbcTemplate.update("INSERT INTO FACEBOOK_EXCLUSIONS (PLAYER_ID, GAME_TYPE, REASON) VALUES (:p,'SLOTS','DENIED')", withP(3));

        final List<SessionKey> keys=newArrayList();
        keys.add(addKey(1));
        keys.add(addKey(2));

        facebookExclusionsDao.resetFacebookExclusions(keys);

        assertThat(dwJdbcTemplate.queryForInt("select count(*) from FACEBOOK_EXCLUSIONS where PLAYER_ID=:p", withP(1)), is(0));
        assertThat(dwJdbcTemplate.queryForInt("select count(*) from FACEBOOK_EXCLUSIONS where PLAYER_ID=:p", withP(2)), is(0));
        assertThat(dwJdbcTemplate.queryForInt("select count(*) from FACEBOOK_EXCLUSIONS where PLAYER_ID=:p", withP(3)), is(2));
    }
    private Map<String, Object> withP(Object param){
        final HashMap<String,Object> params = newHashMap();
        params.put("p", param);
        return params;
    }

    private SessionKey addKey(Integer id) {
        final SessionKey e = new SessionKey();
        e.setPlayerId(valueOf(id));
        return e;
    }

    @Test
    public void logFacebookFailureShouldInsertRow(){
        assertThat(dwJdbcTemplate.queryForInt("select count(*) from FACEBOOK_EXCLUSIONS where PLAYER_ID=:p",withP(1)), is(0));
        facebookExclusionsDao.logFailureInSendingFacebookNotification(ONE, "YOUR_MUM");
        final Map<String, Object> exclusion = dwJdbcTemplate.queryForMap("select * From FACEBOOK_EXCLUSIONS where PLAYER_ID=:p",withP(1));
        assertThat((String) exclusion.get("GAME_TYPE"), equalTo("YOUR_MUM"));
        assertThat((String)exclusion.get("REASON"),equalTo(BOUNCED));
    }

    @Test
    public void logFacebookFailureShouldUpdateOptedOut(){
        assertThat(dwJdbcTemplate.queryForInt("select count(*) from FACEBOOK_EXCLUSIONS where PLAYER_ID=:p",withP(1)),is(0));
        dwJdbcTemplate.update("insert into FACEBOOK_EXCLUSIONS values(:p,'YOUR_MUM','OPTED_OUT')",withP(1));

        facebookExclusionsDao.logFailureInSendingFacebookNotification(ONE, "YOUR_MUM");
        final Map<String, Object> exclusion = dwJdbcTemplate.queryForMap("select * From FACEBOOK_EXCLUSIONS where PLAYER_ID=:p",withP(1));
        assertThat((String)exclusion.get("GAME_TYPE"),equalTo("YOUR_MUM"));
        assertThat((String)exclusion.get("REASON"),equalTo(BOUNCED));
    }

}
