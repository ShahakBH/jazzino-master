package com.yazino.engagement.campaign.dao;

import com.google.common.collect.Lists;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@Transactional
@TransactionConfiguration
public class CampaignContentDaoIntegrationTest {
    public static final long DAY_0_PROGRESSIVE_TOPUP_AMOUNT = 2500l;
    public static final BigDecimal PLAYER_ONE_DAY0 = BigDecimal.ONE;
    public static final BigDecimal PLAYER_TWO_DAY1 = BigDecimal.TEN;
    public static final BigDecimal PLAYER_THREE_DAY2 = BigDecimal.valueOf(2l);
    public static final BigDecimal PLAYER_FOUR_DAY3 = BigDecimal.valueOf(3l);
    public static final BigDecimal PLAYER_FIVE_DAY4 = BigDecimal.valueOf(4l);
    public static final BigDecimal PLAYER_SIX_DAY4_NOT_PLAYED_YESTERDAY = BigDecimal.valueOf(6l);

    public static final Long CAMPAIGN_ID = -2135l;
    public static final long CAMPAIGN_RUN_ID = -223l;
    public static final Timestamp RUN_TS = new Timestamp(new DateTime().withMillisOfSecond(0).getMillis());
    public static final String PLAYER_ONE_DISPLAY_NAME = "Bubble Bobble";
    public static final String PLAYER_TWO_DISPLAY_NAME = "Jorge Rodrigues";

    private CampaignContentDao underTest;

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate externalDwJdbcTemplate;

    @Autowired
    @Qualifier("dwJdbcTemplate")
    private JdbcTemplate dwJdbcTemplate;

    private NamedParameterJdbcTemplate namedMarketingTemplate;
    @Autowired
    private CampaignRunDao campaignRunDao;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2013, 10, 15, 15, 0).getMillis());
        this.namedMarketingTemplate = new NamedParameterJdbcTemplate(externalDwJdbcTemplate);
        underTest = new CampaignContentDao(externalDwJdbcTemplate);
        externalDwJdbcTemplate.update("DELETE FROM SEGMENT_SELECTION WHERE CAMPAIGN_RUN_ID=?", CAMPAIGN_RUN_ID);
        dwJdbcTemplate.update("DELETE FROM CAMPAIGN_RUN ");
        externalDwJdbcTemplate.update("DELETE FROM LOBBY_USER");
        externalDwJdbcTemplate.update("DELETE FROM PLAYER_PROMOTION_STATUS");
        dwJdbcTemplate.update("DELETE FROM CAMPAIGN_DEFINITION WHERE ID=?", CAMPAIGN_ID);
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testFillProgressiveBonusAmount() throws Exception {
        createPlayerPromotionStatusRecord(PLAYER_ONE_DAY0, 0, new DateTime().minusHours(15));
        createPlayerPromotionStatusRecord(PLAYER_TWO_DAY1, 1, new DateTime().minusHours(15));
        createPlayerPromotionStatusRecord(PLAYER_THREE_DAY2, 2, new DateTime().minusHours(15));
        createPlayerPromotionStatusRecord(PLAYER_FOUR_DAY3, 3, new DateTime().minusHours(15));
        createPlayerPromotionStatusRecord(PLAYER_FIVE_DAY4, 4, new DateTime().minusHours(15));
        createPlayerPromotionStatusRecord(PLAYER_SIX_DAY4_NOT_PLAYED_YESTERDAY, 4, new DateTime().minusDays(15));

        createCampaignAndCampaignRunRecords();
        final List<PlayerWithContent> segmentSelection = Lists.newArrayList(
                new PlayerWithContent(PLAYER_ONE_DAY0),
                new PlayerWithContent(PLAYER_TWO_DAY1),
                new PlayerWithContent(PLAYER_THREE_DAY2),
                new PlayerWithContent(PLAYER_FOUR_DAY3),
                new PlayerWithContent(PLAYER_FIVE_DAY4),
                new PlayerWithContent(PLAYER_SIX_DAY4_NOT_PLAYED_YESTERDAY));

        campaignRunDao.addPlayers(CAMPAIGN_RUN_ID, segmentSelection, false);


        underTest.fillProgressiveBonusAmount(CAMPAIGN_RUN_ID);

        Assert.assertThat(
                getProgressiveBonusAmountForPlayer(CAMPAIGN_RUN_ID, PLAYER_ONE_DAY0),
                CoreMatchers.is(IsEqual.equalTo(DAY_0_PROGRESSIVE_TOPUP_AMOUNT)));
        Assert.assertThat(
                getProgressiveBonusAmountForPlayer(CAMPAIGN_RUN_ID, PLAYER_TWO_DAY1),
                CoreMatchers.is(IsEqual.equalTo(3000l)));
        Assert.assertThat(
                getProgressiveBonusAmountForPlayer(CAMPAIGN_RUN_ID, PLAYER_THREE_DAY2),
                CoreMatchers.is(IsEqual.equalTo(3500l)));
        Assert.assertThat(
                getProgressiveBonusAmountForPlayer(CAMPAIGN_RUN_ID, PLAYER_FOUR_DAY3),
                CoreMatchers.is(IsEqual.equalTo(4000l)));
        Assert.assertThat(
                getProgressiveBonusAmountForPlayer(CAMPAIGN_RUN_ID, PLAYER_FIVE_DAY4),
                CoreMatchers.is(IsEqual.equalTo(5000l)));
        Assert.assertThat(
                getProgressiveBonusAmountForPlayer(CAMPAIGN_RUN_ID, PLAYER_SIX_DAY4_NOT_PLAYED_YESTERDAY),
                CoreMatchers.is(IsEqual.equalTo(2500l)));
    }

    @Test
    public void fillDisplayNameShouldSetDisplayNameInSegmentSelection() {
        createCampaignAndCampaignRunRecords();

        externalDwJdbcTemplate.update(
                "INSERT INTO LOBBY_USER (PLAYER_ID, DISPLAY_NAME, PROVIDER_NAME, RPX_PROVIDER, EXTERNAL_ID) VALUES (?,?,?,?,?)",
                PLAYER_ONE_DAY0,
                PLAYER_ONE_DISPLAY_NAME,
                "YAZINO",
                "YAZINO",
                "akldshaksudgiqw");
        externalDwJdbcTemplate.update(
                "INSERT INTO LOBBY_USER (PLAYER_ID, DISPLAY_NAME, PROVIDER_NAME, RPX_PROVIDER, EXTERNAL_ID) VALUES (?,?,?,?,?)",
                PLAYER_TWO_DAY1,
                PLAYER_TWO_DISPLAY_NAME,
                "YAZINO",
                "YAZINO",
                "akldshaksudgiqw");

        campaignRunDao.addPlayers(CAMPAIGN_RUN_ID, Lists.newArrayList(new PlayerWithContent(PLAYER_ONE_DAY0),
                                                                      new PlayerWithContent(PLAYER_TWO_DAY1)),
                                  false);

        underTest.fillDisplayName(CAMPAIGN_RUN_ID);
        assertThat(getDisplayNameForPlayer(CAMPAIGN_RUN_ID, PLAYER_ONE_DAY0), is(PLAYER_ONE_DISPLAY_NAME));
        assertThat(getDisplayNameForPlayer(CAMPAIGN_RUN_ID, PLAYER_TWO_DAY1), is(PLAYER_TWO_DISPLAY_NAME));
    }

    private void createCampaignAndCampaignRunRecords() {
        dwJdbcTemplate.update("INSERT INTO CAMPAIGN_DEFINITION VALUES (?,?,?,?,?,?)", CAMPAIGN_ID, "CampaignContentDaoIntegrationTest", "SELECT 1", 0, true,false);
        insertCampaignRunRecord();
    }


    private int createPlayerPromotionStatusRecord(final BigDecimal playerId, final int consecutiveDaysPlayed,
                                                  final DateTime lastPlayed) {

        final Map<String, Object> arguments = newHashMap();
        arguments.put("playerId", playerId);
        arguments.put("consecutiveDaysPlayed", consecutiveDaysPlayed);
        arguments.put("lastPlayed", lastPlayed.toDate());
        arguments.put("lastTopup", null);
        arguments.put("topUpAcknowledged", Boolean.FALSE);

        return namedMarketingTemplate.update(
                "INSERT INTO PLAYER_PROMOTION_STATUS "
                        + "(PLAYER_ID, LAST_TOPUP_DATE, LAST_PLAYED_DATE, CONSECUTIVE_PLAY_DAYS, TOP_UP_ACKNOWLEDGED) "
                        + "VALUES (:playerId, :lastTopup, :lastPlayed, :consecutiveDaysPlayed, :topUpAcknowledged) "
                , arguments);
    }

    private long getProgressiveBonusAmountForPlayer(final long campaignRunId, final BigDecimal playerId) {
        return externalDwJdbcTemplate.queryForLong(
                "SELECT progressive_bonus FROM SEGMENT_SELECTION WHERE CAMPAIGN_RUN_ID = ? AND PLAYER_ID=?",
                campaignRunId, playerId);
    }

    private String getDisplayNameForPlayer(final long campaignRunId, final BigDecimal playerId) {
        return externalDwJdbcTemplate.queryForObject(
                "SELECT display_name FROM SEGMENT_SELECTION WHERE CAMPAIGN_RUN_ID = ? AND PLAYER_ID=?", String.class,
                campaignRunId, playerId);
    }

    private int insertCampaignRunRecord() {
        return dwJdbcTemplate.update(
                "INSERT INTO CAMPAIGN_RUN (ID, CAMPAIGN_ID, RUN_TS) VALUES (?,?,?)",
                CAMPAIGN_RUN_ID, CAMPAIGN_ID, RUN_TS);
    }

}
