package com.yazino.promotions;

import com.google.common.base.Joiner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import strata.server.lobby.api.promotion.PromotionType;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.platform.Platform.WEB;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.CREDITCARD;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.ITUNES;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static utils.ParamBuilder.emptyParams;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
@DirtiesContext
public class MysqlBuyChipsPromotionDefinitionDaoIntegrationTest {

    public static final String PROMOTION_NAME = "integration Test";
    public static final String IN_GAME_MESSAGE = "inGameMessage";
    public static final String IN_GAME_HEADER = "inGameHeader";
    public static final String ROLLOVER_HEADER = "rolloverHeader";
    public static final String ROLL_OVER_TEXT = "rollOverText";
    public static final int MAX_REWARDS = 2;
    public static final int VALID_FOR_HOURS = 168;
    public static final int PRIORITY = 1;
    public static final List<Platform> PLATFORMS = asList(Platform.ANDROID, Platform.IOS);
    public static final long CAMPAIGN_ID = 21l;
    private static final List<PaymentPreferences.PaymentMethod> PAYMENT_METHODS = asList(CREDITCARD, ITUNES);
    public static final PromotionType PROMOTION_TYPE = PromotionType.BUY_CHIPS;
    public static final int TOP_UP_AMOUNT = 2599;

    @Autowired
    @Qualifier("dwNamedJdbcTemplate")
    private NamedParameterJdbcTemplate template;

    private MysqlBuyChipsPromotionDefinitionDao underTest;

    private PromotionChipsPercentageDao promotionChipsPercentageDao;
    private Map<Integer, BigDecimal> chipsPackagePercentages = new LinkedHashMap<Integer, BigDecimal>();

    @Before
    public void setUp() throws Exception {
        template.update("delete from PROMOTION_DEFINITION",emptyParams());
        promotionChipsPercentageDao = new PromotionChipsPercentageMysqlDao(template);
        underTest = new MysqlBuyChipsPromotionDefinitionDao(template, promotionChipsPercentageDao);

        chipsPackagePercentages.put(1, new BigDecimal("100").setScale(2));
        chipsPackagePercentages.put(2, new BigDecimal("200").setScale(2));

    }

    @Test
    public void saveShouldWritePromotionDefinitionRecordForBuyChipsForm() {

        final BuyChipsForm defaultBuyChipsFormForSave = getDefaultBuyChipsFormForSave();
        defaultBuyChipsFormForSave.setAllPlayers(true);
        final Long actualPromotionId = underTest.save(defaultBuyChipsFormForSave);

        final Map<String, Object> params=newHashMap();
        params.put("promotion", actualPromotionId);
        int campaignId = template.queryForInt("select campaign_id from PROMOTION_DEFINITION where id=:promotion", params);
        final BuyChipsForm actual = underTest.getForm(new Long(campaignId));

        assertThat(actual.getPromotionDefinitionId(), is(equalTo(actualPromotionId)));
        assertThat(actual.getName(), is(equalTo(PROMOTION_NAME)));
        assertThat(actual.getValidForHours(), is(equalTo(VALID_FOR_HOURS)));
        assertThat(actual.getPriority(), is(equalTo(PRIORITY)));
        assertThat(actual.getPlatforms(), is(equalTo(PLATFORMS)));
        assertThat(actual.getPromoType(), is(equalTo(PROMOTION_TYPE)));
        assertThat(actual.isAllPlayers(), is(true));
    }


    @Test
    public void saveShouldWritePromotionConfigurationRecordsForBuyChipsForm() {
        final Long actualPromotionId = underTest.save(getDefaultBuyChipsFormForSave());
        final HashMap<String, String> actualPromoConfig = fetchPromotionConfigById(actualPromotionId);

        assertThat(actualPromoConfig.get(PromotionConfigKeyEnum.IN_GAME_NOTIFICATION_HEADER_KEY.getDescription()), is(IsEqual.equalTo(IN_GAME_HEADER)));
        assertThat(actualPromoConfig.get(PromotionConfigKeyEnum.IN_GAME_NOTIFICATION_MSG_KEY.getDescription()), is(IsEqual.equalTo(IN_GAME_MESSAGE)));
        assertThat(actualPromoConfig.get(PromotionConfigKeyEnum.ROLLOVER_HEADER_KEY.getDescription()), is(IsEqual.equalTo(ROLLOVER_HEADER)));
        assertThat(actualPromoConfig.get(PromotionConfigKeyEnum.ROLLOVER_TEXT_KEY.getDescription()), is(IsEqual.equalTo(ROLL_OVER_TEXT)));
        assertThat(Integer.valueOf(actualPromoConfig.get(PromotionConfigKeyEnum.MAX_REWARDS_KEY.getDescription())), is(IsEqual.equalTo(MAX_REWARDS)));
        assertThat(actualPromoConfig.get(PromotionConfigKeyEnum.PAYMENT_METHODS.getDescription()), is(Joiner.on(",").join(PAYMENT_METHODS).toString()));
    }

    @Test
    public void saveShouldWritePromotionChipsPercentage() {

        final Long promotionDefinitionId = underTest.save(getDefaultBuyChipsFormForSave());

        Map<Integer, BigDecimal> chipsPercentagesFromDb = promotionChipsPercentageDao.getChipsPercentages(promotionDefinitionId);

        assertThat(chipsPercentagesFromDb, is(chipsPackagePercentages));
    }

    @Test
    public void getBuyChipsFormShouldRetrieveBuyChipsFormWithValues() {
        final BuyChipsForm expected = getDefaultBuyChipsFormForSave();
        final Long promoDefId = underTest.save(expected);
        expected.setPromotionDefinitionId(promoDefId);

        final BuyChipsForm actual = underTest.getForm(CAMPAIGN_ID);
        Assert.assertThat(actual, CoreMatchers.is(IsEqual.equalTo(expected)));
    }

    @Test
    public void updateShouldUpdatePromotionRecordsForBuyChips() {
        final Long promoDefId = underTest.save(getDefaultBuyChipsFormForSave());

        final BuyChipsForm expected = getDefaultBuyChipsFormForSave();
        expected.setPromotionDefinitionId(promoDefId);
        expected.setName("expected name");
        expected.setCampaignId(CAMPAIGN_ID);
        expected.setValidForHours(100);
        expected.setRolloverText("updated text");
        expected.setInGameNotificationMsg("updated message");
        expected.setInGameNotificationHeader("change of header");
        expected.setRolloverHeader("updated header");
        expected.setPlatforms(asList(Platform.ANDROID));
        expected.setPromoType(PromotionType.BUY_CHIPS);

        underTest.update(expected);

        final BuyChipsForm actual = underTest.getForm(CAMPAIGN_ID);
        assertThat(actual.getPlatforms(), equalTo(asList(Platform.ANDROID)));
        assertThat(actual.getValidForHours(), equalTo(100));
        assertThat(actual.getRolloverHeader(), equalTo("updated header"));
        assertThat(actual.getRolloverText(), equalTo("updated text"));
        assertThat(actual.getRolloverText(), equalTo("updated text"));
        assertThat(actual.getCampaignId(), equalTo(CAMPAIGN_ID));
        assertThat(actual.getName(), equalTo("expected name"));
        assertThat(actual.getPromoType(), equalTo(PromotionType.BUY_CHIPS));
    }


    @Test
    public void updateShouldInsertRecordsIfNonePreviouslyExisted() {

        final BuyChipsForm expected = getDefaultBuyChipsFormForSave();
        expected.setPromotionDefinitionId(null);
        expected.setName("expected name");
        expected.setCampaignId(CAMPAIGN_ID);
        expected.setValidForHours(100);
        expected.setRolloverText("updated text");
        expected.setInGameNotificationMsg("updated message");
        expected.setInGameNotificationHeader("change of header");
        expected.setRolloverHeader("updated header");
        expected.setPlatforms(asList(Platform.ANDROID));

        underTest.update(expected);

        final BuyChipsForm actual = underTest.getForm(CAMPAIGN_ID);
        assertThat(actual.getName(), equalTo("expected name"));
        assertThat(actual.getPlatforms(), equalTo(asList(Platform.ANDROID)));
        assertThat(actual.getValidForHours(), equalTo(100));
        assertThat(actual.getRolloverHeader(), equalTo("updated header"));
        assertThat(actual.getRolloverText(), equalTo("updated text"));
        assertThat(actual.getRolloverText(), equalTo("updated text"));
        assertThat(actual.getCampaignId(), equalTo(CAMPAIGN_ID));
    }


    @Test
    public void getPlatformListAsStringShouldReturnListOfPlatformsAsString() {
        final String actual = underTest.getPlatformListAsString(asList(Platform.IOS, WEB, Platform.ANDROID));
        Assert.assertThat(actual, CoreMatchers.is(IsEqual.equalTo("IOS,WEB,ANDROID")));
    }


    @Test
    public void getPlatformListAsStringShouldReturnCommaSeparatedString() {
        String platformListAsString = underTest.getPlatformListAsString(PLATFORMS);
        assertThat(platformListAsString, is("ANDROID,IOS"));
    }

    @Test
    public void getPlatformListAsStringShouldReturnEmptyStringIfPlatFormsListIsEmpty() {
        String platformListAsString = underTest.getPlatformListAsString(new ArrayList<Platform>());
        assertThat(platformListAsString, is(""));
    }


    private BuyChipsForm getDefaultBuyChipsFormForSave() {

        BuyChipsForm buyChipsForm = new BuyChipsForm();
        buyChipsForm.setPromotionDefinitionId(null);
        buyChipsForm.setName(PROMOTION_NAME);
        buyChipsForm.setInGameNotificationHeader(IN_GAME_HEADER);
        buyChipsForm.setInGameNotificationMsg(IN_GAME_MESSAGE);
        buyChipsForm.setRolloverHeader(ROLLOVER_HEADER);
        buyChipsForm.setRolloverText(ROLL_OVER_TEXT);
        buyChipsForm.setMaxRewards(MAX_REWARDS);
        buyChipsForm.setValidForHours(VALID_FOR_HOURS);
        buyChipsForm.setPriority(PRIORITY);
        buyChipsForm.setPlatforms(PLATFORMS);
        buyChipsForm.setCampaignId(CAMPAIGN_ID);
        buyChipsForm.setPromoType(PROMOTION_TYPE);

        buyChipsForm.setPaymentMethods(PAYMENT_METHODS);

        buyChipsForm.setChipsPackagePercentages(chipsPackagePercentages);

        return buyChipsForm;
    }

    private BuyChipsForm fetchBuyChipsPromotionDefinitionByName(final String name) {
        final HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("name", PROMOTION_NAME);

        return template.queryForObject("SELECT * FROM PROMOTION_DEFINITION WHERE NAME = :name", paramMap, new RowMapper<BuyChipsForm>() {
            @Override
            public BuyChipsForm mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final BuyChipsForm buyChipsForm = getBuyChipsFormMapPromoDefinitionRow(rs);
                return buyChipsForm;
            }
        });
    }

    private DailyAwardForm fetchDailyAwardPromotionDefinitionByName(final String name) {
        final HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("name", PROMOTION_NAME);

        return template.queryForObject("SELECT * FROM PROMOTION_DEFINITION WHERE NAME = :name", paramMap, new RowMapper<DailyAwardForm>() {
            @Override
            public DailyAwardForm mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final DailyAwardForm dailyAwardForm = getDailyAwardFormMapPromoDefinitionRow(rs);
                return dailyAwardForm;
            }
        });
    }

    private HashMap<String, String> fetchPromotionConfigById(final Long id) {
        final HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("ID", id);

        final HashMap<String, String> promoConfig = template.query("SELECT CONFIG_KEY, CONFIG_VALUE FROM CAMPAIGN_PROMOTION_CONFIG WHERE PROMOTION_DEFINITION_ID = :ID", paramMap, new ResultSetExtractor<HashMap<String, String>>() {
            @Override
            public HashMap<String, String> extractData(final ResultSet rs) throws SQLException, DataAccessException {
                final HashMap<String, String> contentMap = new HashMap<String, String>();
                while (rs.next()) {
                    contentMap.put(rs.getString("CONFIG_KEY"), rs.getString("CONFIG_VALUE"));
                }
                return contentMap;
            }
        });
        return promoConfig;

    }

    private BuyChipsForm getBuyChipsFormMapPromoDefinitionRow(final ResultSet rs) throws SQLException {
        final BuyChipsForm buyChipsForm = new BuyChipsForm();
        buyChipsForm.setPromotionDefinitionId(rs.getLong("ID"));
        buyChipsForm.setCampaignId(rs.getLong("CAMPAIGN_ID"));
        buyChipsForm.setName(rs.getString("NAME"));
        buyChipsForm.setValidForHours(rs.getInt("VALID_FOR_HOURS"));
        buyChipsForm.setPriority(rs.getInt("PRIORITY"));
        buyChipsForm.setPromoType(PromotionType.valueOf(rs.getString("PROMOTION_TYPE")));

        getPlatformListFromString(rs.getString("PLATFORMS"), buyChipsForm);
        return buyChipsForm;
    }

    private DailyAwardForm getDailyAwardFormMapPromoDefinitionRow(final ResultSet rs) throws SQLException {
        final DailyAwardForm dailyAwardForm = new DailyAwardForm();
        dailyAwardForm.setPromotionDefinitionId(rs.getLong("ID"));
        dailyAwardForm.setCampaignId(rs.getLong("CAMPAIGN_ID"));
        dailyAwardForm.setName(rs.getString("NAME"));
        dailyAwardForm.setValidForHours(rs.getInt("VALID_FOR_HOURS"));
        dailyAwardForm.setPriority(rs.getInt("PRIORITY"));
        dailyAwardForm.setPromoType(PromotionType.valueOf(rs.getString("PROMOTION_TYPE")));
        getPlatformListFromString(rs.getString("PLATFORMS"), dailyAwardForm);
        return dailyAwardForm;
    }

    private void getPlatformListFromString(final String platformsString, final PromotionForm promotionForm) throws SQLException {
        final String[] platformArray = platformsString.split(",");
        final List<Platform> platformList = new ArrayList<Platform>();

        for (String platformString : platformArray) {
            platformList.add(Platform.valueOf(platformString));
        }

        promotionForm.setPlatforms(platformList);
    }


}
