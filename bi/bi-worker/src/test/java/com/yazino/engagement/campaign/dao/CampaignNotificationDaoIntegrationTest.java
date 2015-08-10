package com.yazino.engagement.campaign.dao;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.EmailTarget;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.campaign.domain.NotificationCustomField;
import com.yazino.platform.table.GameTypeInformation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import strata.server.operations.repository.GameTypeRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@TransactionConfiguration
public class CampaignNotificationDaoIntegrationTest {


    public static final String PLAYER1_EMAIL_ADDRESS = "player1@yazino.com";
    public static final String PLAYER6_EMAIL_ADDRESS = "player6@yazino.com";
    public static final String PLAYER2_EMAIL_ADDRESS = "player2@yazino.com";
    public static final String PLAYER_1_DISPLAY_NAME = "player1";
    @Autowired
    private CampaignNotificationDao campaignNotificationDao;

    @Autowired
    private FacebookExclusionsDao facebookExclusionsDao;

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate dwJdbcTemplate;


    @Autowired
    private GameTypeRepository gameTypeRepository;

    public static final String EMPTY_STRING = "";
    public static final String PLAYER_ID1 = "-1";
    public static final String PLAYER_ID2 = "-2";
    public static final String PLAYER_ID3 = "-3";
    public static final String PLAYER_ID4 = "-4";
    public static final String PLAYER_ID5 = "-5";
    public static final String PLAYER_ID6 = "-6";
    public static final String PLAYER_ID7 = "-7";
    public static final String PLAYER_ID8 = "-8";
    public static final String PLAYER_ID9 = "-9";
    public static final String PLAYER_ID10 = "-10";

    private Long campaignRunId = -12345L;

    private String player1Device1Token = "1A2B3C4D";
    private String player2Device1Token = "2A3B4C45D";
    private String player2Device2Token = "3A4B5C6D";

    private static final String GAME_TYPE = "SLOTS";
    private static final String BUNDLE = "com.yazino.YazinoApp";

    @Before
    public void setUp() throws Exception {
        dwJdbcTemplate.execute("delete from LOBBY_USER");
        dwJdbcTemplate.execute("delete from SEGMENT_SELECTION");
        dwJdbcTemplate.execute("delete from FACEBOOK_EXCLUSIONS");
        dwJdbcTemplate.update("delete from EMAIL_VALIDATION where email_address like 'player%@yazino.com'");
    }

    @Test
    public void getEligiblePlayerTargetsShouldReturnContentForEmail() {
        insertLobbyUserInfo();
        insertCampaign();

        dwJdbcTemplate.execute("INSERT INTO SEGMENT_SELECTION(CAMPAIGN_RUN_ID, PLAYER_ID, PROGRESSIVE_BONUS, CONTENT) VALUES "
                + "(" + campaignRunId + "," + PLAYER_ID1 + "," + "1" + ",'{\"REG_PLT\":\"CANVAS\"}')");
        final List<EmailTarget> eligibleEmailTargets = campaignNotificationDao.getEligibleEmailTargets(campaignRunId);
        assertThat(eligibleEmailTargets.size(), is(1));
        final Map<String, Object> content = newHashMap();
        content.put("REG_PLT", "CANVAS");
        assertThat(eligibleEmailTargets.get(0).getContent(), equalTo(content));
    }

    @Test
    public void getEligiblePlayerTargetsShouldReturnSixPlayerTargetsForFacebook() {

        insertLobbyUserInfo();
        insertCampaign();
        insertDataIntoSegmentSelect();

        List<PlayerTarget> playerTargets = campaignNotificationDao.getEligiblePlayerTargets(campaignRunId, ChannelType.FACEBOOK_APP_TO_USER_REQUEST);
        assertThat(playerTargets.size(), is(6));

        List<PlayerTarget> expectedPlayerTargets = createPlayerTargetsForFacebook();

        assertThat(playerTargets, containsInAnyOrder(expectedPlayerTargets.toArray()));
    }

    @Test
    public void shouldHandleContentFromSegmentSql() {
        insertLobbyUserInfo();
        insertCampaign();
        dwJdbcTemplate.execute("INSERT INTO SEGMENT_SELECTION(CAMPAIGN_RUN_ID, PLAYER_ID, PROGRESSIVE_BONUS, CONTENT) VALUES "
                + "(" + campaignRunId + "," + PLAYER_ID1 + "," + "1" + ", '{\"sender name\":\"john rae\"}'),"
                + "(" + campaignRunId + "," + PLAYER_ID2 + "," + "1" + ", '{\"sender id\":1}')");

        final List<PlayerTarget> eligiblePlayerTargets = campaignNotificationDao.getEligiblePlayerTargets(campaignRunId,
                ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION);
        assertThat(eligiblePlayerTargets.size(), is(4));
        int johnraes = 0;
        int senderIds1 = 0;
        for (PlayerTarget eligiblePlayerTarget : eligiblePlayerTargets) {
            if ("john rae".equals(eligiblePlayerTarget.getCustomData().get("sender name"))) {
                johnraes++;
            }
            if ("1".equals(eligiblePlayerTarget.getCustomData().get("sender id"))) {
                senderIds1++;
            }
        }
        assertThat(johnraes, equalTo(2));
        assertThat(senderIds1, equalTo(2));
    }

    @Test
    public void getEligiblePlayerTargetsShouldReturnFivePlayerTargetsForEmailVision() {
        insertLobbyUserInfo();
        insertCampaign();
        insertDataIntoSegmentSelect();
        dwJdbcTemplate.execute("INSERT INTO SEGMENT_SELECTION(CAMPAIGN_RUN_ID, PLAYER_ID, PROGRESSIVE_BONUS) VALUES "
                + "(" + campaignRunId + "," + PLAYER_ID7 + "," + "1" + "),"
                + "(" + campaignRunId + "," + PLAYER_ID8 + "," + "2" + "),"
                + "(" + campaignRunId + "," + PLAYER_ID9 + "," + "3" + ")");

        insertEmailAddressIntoEmailValidation(PLAYER1_EMAIL_ADDRESS, "V");
        //player2 no record but should be in because he is facebook
        insertEmailAddressIntoEmailValidation(PLAYER6_EMAIL_ADDRESS, "V");
        insertEmailAddressIntoEmailValidation("player7@yazino.com", "I");
        insertEmailAddressIntoEmailValidation("player8@yazino.com", "M");
        insertEmailAddressIntoEmailValidation("player9@yazino.com", "A");

        List<EmailTarget> playerTargets = campaignNotificationDao.getEligibleEmailTargets(campaignRunId);

        assertThat(playerTargets.size(), is(5));

        EmailTarget player1 = new EmailTarget(PLAYER1_EMAIL_ADDRESS, "player1", null);
        EmailTarget player2 = new EmailTarget(PLAYER2_EMAIL_ADDRESS, "player2", null);
        EmailTarget player6 = new EmailTarget(PLAYER6_EMAIL_ADDRESS, "player6", null);

        assertThat(playerTargets, hasItems(player1, player2, player6));
    }

    @Test
    public void getEligiblePlayerTargetsShouldNotReturnPlayerTargetsWithNullEmailAddressForEmailVision() {
        insertLobbyUserInfo();
        insertCampaign();
        insertDataIntoSegmentSelect();
        dwJdbcTemplate.update("UPDATE LOBBY_USER SET email_address = ? where player_id = ?", null, new BigDecimal(PLAYER_ID2));
        dwJdbcTemplate.execute("INSERT INTO SEGMENT_SELECTION(CAMPAIGN_RUN_ID, PLAYER_ID, PROGRESSIVE_BONUS) VALUES "
                + "(" + campaignRunId + "," + PLAYER_ID7 + "," + "1" + "),"
                + "(" + campaignRunId + "," + PLAYER_ID8 + "," + "2" + "),"
                + "(" + campaignRunId + "," + PLAYER_ID9 + "," + "3" + ")");

        insertEmailAddressIntoEmailValidation(PLAYER1_EMAIL_ADDRESS, "V");
        //player2 no record but should be in because he is facebook but will not be because his email_address is null
        insertEmailAddressIntoEmailValidation(PLAYER6_EMAIL_ADDRESS, "V");
        insertEmailAddressIntoEmailValidation("player7@yazino.com", "I");
        insertEmailAddressIntoEmailValidation("player8@yazino.com", "M");
        insertEmailAddressIntoEmailValidation("player9@yazino.com", "A");

        List<EmailTarget> playerTargets = campaignNotificationDao.getEligibleEmailTargets(campaignRunId);

        assertThat(playerTargets.size(), is(4));

        EmailTarget player1 = new EmailTarget(PLAYER1_EMAIL_ADDRESS, "player1", null);
        EmailTarget player6 = new EmailTarget(PLAYER6_EMAIL_ADDRESS, "player6", null);

        assertThat(playerTargets, hasItems(player1, player6));
    }

    private int insertEmailAddressIntoEmailValidation(final String emailAddress, final String status) {
        return dwJdbcTemplate.update("INSERT INTO EMAIL_VALIDATION (EMAIL_ADDRESS, STATUS) VALUES (?,?)", emailAddress, status);
    }

    @Test
    public void getEligiblePlayerTargetsShouldDisplayNameSetFacebook() {
        insertLobbyUserInfo();
        insertCampaign();
        dwJdbcTemplate.update("INSERT INTO SEGMENT_SELECTION(CAMPAIGN_RUN_ID, PLAYER_ID, PROGRESSIVE_BONUS, DISPLAY_NAME) VALUES (?,?,?,?)", campaignRunId,
                new BigDecimal(PLAYER_ID1), 1, PLAYER_1_DISPLAY_NAME);

        List<PlayerTarget> facebookPlayerTargets = campaignNotificationDao.getEligiblePlayerTargets(campaignRunId, ChannelType.FACEBOOK_APP_TO_USER_REQUEST);

        PlayerTarget facebookPlayer = new PlayerTarget("HIGH_STAKES", "-100", new BigDecimal(PLAYER_ID1), EMPTY_STRING, EMPTY_STRING, getCustomDataMap("1", PLAYER_1_DISPLAY_NAME));

        Assert.assertThat(facebookPlayerTargets, hasItem(facebookPlayer));
    }

    private Map<String, String> getCustomDataMap(final String progressiveValue, final String displayName) {
        Map<String, String> customData = new HashMap<>();
        customData.put(NotificationCustomField.PROGRESSIVE.name(), progressiveValue);
        if (displayName != null) {
            customData.put(NotificationCustomField.DISPLAY_NAME.name(), displayName);
        }

        return customData;
    }

    @Test
    public void getEligiblePlayerTargetsShouldHaveProgressiveSetForFacebook() {

        insertLobbyUserInfo();
        insertCampaign();
        insertDataIntoSegmentSelect();

        List<PlayerTarget> playerTargets = campaignNotificationDao.getEligiblePlayerTargets(campaignRunId, ChannelType.FACEBOOK_APP_TO_USER_REQUEST);
        assertThat(playerTargets.size(), is(6));

        PlayerTarget player1Device1 = new PlayerTarget("HIGH_STAKES", "-100", new BigDecimal(PLAYER_ID1), EMPTY_STRING, EMPTY_STRING, getCustomDataMap("1", null));

        assertThat(playerTargets, hasItem(player1Device1));
    }

    //should look up opt-outs and join them onto the
    @Test
    public void getEligiblePlayerTargetsForFacebookShouldIgnorePlayersWhoHaveExceptions() {

        insertLobbyUserInfo();
        insertCampaign();
        insertDataIntoSegmentSelect();

        facebookExclusionsDao.logFailureInSendingFacebookNotification(new BigDecimal(PLAYER_ID1), GAME_TYPE);
        facebookExclusionsDao.logFailureInSendingFacebookNotification(new BigDecimal(PLAYER_ID2), "OTHER_GAME");

        List<PlayerTarget> playerTargets = campaignNotificationDao.getEligiblePlayerTargets(campaignRunId,
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST);
        assertThat(playerTargets.size(), is(5));

        List<PlayerTarget> expectedPlayerTargets = createPlayerTargetsForFacebook();
        List<PlayerTarget> filteredTargets = newArrayList();
        //remove player_1
        for (PlayerTarget expectedPlayerTarget : expectedPlayerTargets) {
            if (!(expectedPlayerTarget.getPlayerId().equals(new BigDecimal(PLAYER_ID1)) && expectedPlayerTarget.getGameType().equals(GAME_TYPE))) {
                filteredTargets.add(expectedPlayerTarget);
            }
        }

        assertThat(playerTargets, containsInAnyOrder(filteredTargets.toArray()));
    }

    @Test
    public void getEligiblePlayerTargetsShouldIgnorePlayersWithMultipleExclusions() {
        insertLobbyUserInfo();
        insertCampaign();
        insertDataIntoSegmentSelect();

        facebookExclusionsDao.logFailureInSendingFacebookNotification(new BigDecimal(PLAYER_ID1), GAME_TYPE);
        facebookExclusionsDao.logFailureInSendingFacebookNotification(new BigDecimal(PLAYER_ID1), "OTHER_GAME");
        facebookExclusionsDao.logFailureInSendingFacebookNotification(new BigDecimal(PLAYER_ID1), "AND_ANOTHER_GAME");

        List<PlayerTarget> playerTargets = campaignNotificationDao.getEligiblePlayerTargets(campaignRunId,
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST);
        assertThat(playerTargets.size(), is(5));

        List<PlayerTarget> expectedPlayerTargets = createPlayerTargetsForFacebook();
        List<PlayerTarget> filteredTargets = newArrayList();
        //remove player_1
        for (PlayerTarget expectedPlayerTarget : expectedPlayerTargets) {
            if (!(expectedPlayerTarget.getPlayerId().equals(new BigDecimal(PLAYER_ID1)) && expectedPlayerTarget.getGameType().equals(GAME_TYPE))) {
                filteredTargets.add(expectedPlayerTarget);
            }
        }

        assertThat(playerTargets, containsInAnyOrder(filteredTargets.toArray()));
    }

    @Test
    public void getCustomDataShouldNotAddKeysWithNullValuesToCustomDataMap() {
        insertLobbyUserInfo();
        insertCampaign();
        dwJdbcTemplate.update("INSERT INTO SEGMENT_SELECTION (CAMPAIGN_RUN_ID, PLAYER_ID, PROGRESSIVE_BONUS) VALUES (?,?,?)", campaignRunId, new BigDecimal(PLAYER_ID1), null);

        final List<PlayerTarget> playerTargets = campaignNotificationDao.getEligiblePlayerTargets(campaignRunId, ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION);
        final PlayerTarget playerTarget = playerTargets.get(0);
        assertThat(playerTarget.getCustomData().size(), is(0));
    }

    private List<PlayerTarget> createPlayerTargetsForFacebook() {

        List<PlayerTarget> playerTargets = new ArrayList<PlayerTarget>();

        Map<String, GameTypeInformation> gameConfigurationsMap = gameTypeRepository.getGameTypes();

        for (String gameType : gameConfigurationsMap.keySet()) {
            playerTargets.add(new PlayerTarget(gameType, "-100", new BigDecimal(PLAYER_ID1), EMPTY_STRING, EMPTY_STRING, getCustomDataMap("1", null)));
            playerTargets.add(new PlayerTarget(gameType, "-200", new BigDecimal(PLAYER_ID2), EMPTY_STRING, EMPTY_STRING, getCustomDataMap("2", null)));
            playerTargets.add(new PlayerTarget(gameType, "-300", new BigDecimal(PLAYER_ID3), EMPTY_STRING, EMPTY_STRING, getCustomDataMap("3", null)));
        }
        return playerTargets;
    }

    private void insertLobbyUserInfo() {
        dwJdbcTemplate.execute(String.format(
                "INSERT INTO LOBBY_USER(PLAYER_ID, EXTERNAL_ID, PROVIDER_NAME, RPX_PROVIDER, BLOCKED, EMAIL_ADDRESS, DISPLAY_NAME) VALUES " +
                        "( %s,-100,'facebook','someRpx',false, 'player1@yazino.com', 'player1')," +
                        "( %s,-200,'Facebook','someRpx',false, 'player2@yazino.com', 'player2')," +
                        "( %s,-300,'FACEBOOK','someRpx',false, 'player3@yazino.com', 'player3')," +
                        "( %s,-600,'SomeProvider','someRpx',false, 'player6@yazino.com', 'player6')," +
                        "( %s,null,'FACEBOOK','someRpx',false, 'player7@yazino.com', 'player7')",
                PLAYER_ID1,
                PLAYER_ID2,
                PLAYER_ID3,
                PLAYER_ID6,
                PLAYER_ID7));
    }

    private void insertCampaign() {
    }

    private void insertDataIntoSegmentSelect() {

        dwJdbcTemplate.execute("INSERT INTO SEGMENT_SELECTION(CAMPAIGN_RUN_ID, PLAYER_ID, PROGRESSIVE_BONUS) VALUES "
                + "(" + campaignRunId + "," + PLAYER_ID1 + "," + "1" + "),"
                + "(" + campaignRunId + "," + PLAYER_ID2 + "," + "2" + "),"
                + "(" + campaignRunId + "," + PLAYER_ID3 + "," + "3" + "),"
                + "(" + campaignRunId + "," + PLAYER_ID4 + "," + "4" + "),"
                + "(" + campaignRunId + "," + PLAYER_ID5 + "," + "1" + "),"
                + "(" + campaignRunId + "," + PLAYER_ID6 + "," + "1" + "),"
                + "(" + campaignRunId + "," + PLAYER_ID10 + "," + "1" + ")");
    }
}
