package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.campaign.domain.MessageContentType.MESSAGE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class IosNotificationCampaignConsumerIntegrationTest {

    @Autowired
    private IosNotificationCampaignConsumer underTest;

    public static final String GAME_TYPE = "SLOTS";
    public static final String BUNDLE = "com.yazino.YazinoApp";
    public static final String MESSAGE_VALUE = "Darren and John Testing 8 ";
    public static final String EMPTY_STRING = "";


    private List<PlayerTarget> playerTargets = new ArrayList<PlayerTarget>();

    //  Make Sure that environment.properties are having following values set from production values (Hint: see Support project)
    // strata.worker.yaps.config.pushservice.host=
    // strata.worker.yaps.config.com.yazino.YazinoApp.certificateName=WheelDealProduction.p12

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Test
    @Ignore
    public void handlesShouldSendMessageToIosDevice() {

        if (hasMelAgreedToUseHerIphoneDetailsToSendMessage()) {
            playerTargets.add(createPlayerTargetUsingIphoneDetailsOfMel());
        } else if (youWantToCreateYourOwnTarget()) {
            playerTargets.add(new PlayerTarget("BlackJack",
                                               "externalId",
                                               BigDecimal.TEN,
                                               "<fea2bc62 3821d0ce 3ed66ab5 462ace10 dbc2d4a4 3c83251c dd412b8f a3bc2a46>",
                                               "yazino.Blackjack",
                                               null));
        } else if(youWantToGetYourTargetsFromTheDBThatDoesNotHAveTheRightFields()) {
            playerTargets = getPlayersDetailsFromDb();
        }

        assertThat(playerTargets, is(not(empty())));

        for (PlayerTarget playerTarget : playerTargets) {
            HashMap<String, String> contentMap = newHashMap();
            String firstFewLettersOfDeviceToken = playerTarget.getTargetToken().substring(0, 3);
            contentMap.put(MESSAGE.getKey(), MESSAGE_VALUE + firstFewLettersOfDeviceToken);
            PushNotificationMessage message = new PushNotificationMessage(playerTarget, contentMap, ChannelType.IOS, 123213l);
            underTest.handle(message);
        }
    }

    private List<PlayerTarget> getPlayersDetailsFromDb() {
        // Find user/device details from strataprod assuming test-user has player_id = -1
        return jdbcTemplate.query("SELECT GAME_TYPE, PLAYER_ID, PUSH_TOKEN, BUNDLE  FROM MOBILE_DEVICE WHERE PLAYER_ID = -1",
                new RowMapper<PlayerTarget>() {

                    @Override
                    public PlayerTarget mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new PlayerTarget(rs.getString("GAME_TYPE"), EMPTY_STRING,
                                rs.getBigDecimal("PLAYER_ID"), rs.getString("PUSH_TOKEN"),
                                rs.getString("BUNDLE"), null);
                    }
                });
    }

    private PlayerTarget createPlayerTargetUsingIphoneDetailsOfMel() {
        String playerId = "5";
        String deviceToken = "e645f7c4e84e0cf4b3dd77b63f52c3666a5b4f8e58fa827ffa86bdeb4a7db04a";
        return new PlayerTarget(GAME_TYPE, "12345", new BigDecimal(playerId), deviceToken, BUNDLE, null);
    }

    private boolean hasMelAgreedToUseHerIphoneDetailsToSendMessage() {
        // Ask her politely :)
        return false;
    }

    private boolean youWantToGetYourTargetsFromTheDBThatDoesNotHAveTheRightFields() {
        return false;
    }

    private boolean youWantToCreateYourOwnTarget() {
        return true;
    }
}
