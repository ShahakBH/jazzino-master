package com.yazino.promotions;

import com.yazino.platform.Platform;
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

import static com.yazino.platform.Platform.WEB;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
@DirtiesContext
public class MysqlDailyAwardPromotionDaoIntegrationTest {

    public static final String PROMOTION_NAME = "integration Test";
    public static final int MAX_REWARDS = 2;
    public static final int VALID_FOR_HOURS = 168;
    public static final int PRIORITY = 1;
    public static final List<Platform> PLATFORMS = asList(Platform.ANDROID, Platform.IOS);
    public static final long CAMPAIGN_ID = 21l;
    public static final PromotionType PROMOTION_TYPE = PromotionType.DAILY_AWARD;
    public static final int TOP_UP_AMOUNT = 2599;

    @Autowired
    @Qualifier("dwNamedJdbcTemplate")
    private NamedParameterJdbcTemplate template;

    private MysqlDailyAwardPromotionDao underTest;

    private Map<Integer, BigDecimal> chipsPackagePercentages = new LinkedHashMap<Integer, BigDecimal>();

    @Before
    public void setUp() throws Exception {
        underTest = new MysqlDailyAwardPromotionDao(template);

        chipsPackagePercentages.put(1, new BigDecimal("100").setScale(2));
        chipsPackagePercentages.put(2, new BigDecimal("200").setScale(2));
    }


    @Test
    public void saveAndFetchShouldWriteAndReturnPromotionDefinitionRecordForDailyAwardForm() {

        final Long actualPromotionId = underTest.save(getDefaultDailyAwardFormForSave());

        final DailyAwardForm actual = fetchDailyAwardPromotionDefinitionByName(PROMOTION_NAME);

        assertThat(actual.getPromotionDefinitionId(), is(equalTo(actualPromotionId)));
        assertThat(actual.getName(), is(equalTo(PROMOTION_NAME)));
        assertThat(actual.getValidForHours(), is(equalTo(VALID_FOR_HOURS)));
        assertThat(actual.getPriority(), is(equalTo(PRIORITY)));
        assertThat(actual.getPlatforms(), is(equalTo(PLATFORMS)));
        assertThat(actual.getPromoType(), is(equalTo(PROMOTION_TYPE)));
    }


    @Test
    public void saveShouldWritePromotionConfigurationRecordsForDailyAward() {
        final DailyAwardForm defaultDailyAwardFormForSave = getDefaultDailyAwardFormForSave();
        defaultDailyAwardFormForSave.setAllPlayers(true);
        final Long actualPromotionId = underTest.save(defaultDailyAwardFormForSave);
        final HashMap<String, String> actualPromoConfig = fetchPromotionConfigById(actualPromotionId);

        assertThat(Integer.valueOf(actualPromoConfig.get(PromotionConfigKeyEnum.TOPUP_AMOUNT_KEY.getDescription())), is(equalTo(TOP_UP_AMOUNT)));
        assertThat(actualPromoConfig.get(PromotionConfigKeyEnum.ALL_PLAYERS.getDescription()), is(equalTo("true")));
    }


    @Test
    public void updateShouldUpdatePromotionRecordsForDailyAward() {
        final Long promoDefId = underTest.save(getDefaultDailyAwardFormForSave());

        final DailyAwardForm expected = getDefaultDailyAwardFormForSave();
        expected.setPromotionDefinitionId(promoDefId);
        expected.setName("expected name");
        expected.setCampaignId(CAMPAIGN_ID);
        expected.setValidForHours(100);
        expected.setPlatforms(asList(Platform.ANDROID));
        expected.setPromoType(PROMOTION_TYPE);
        expected.setTopUpAmount(TOP_UP_AMOUNT + 1);
        expected.setMaxRewards(4);
        expected.setAllPlayers(true);

        underTest.update(expected);

        final DailyAwardForm actual = underTest.getForm(CAMPAIGN_ID);
        assertThat(actual.getPlatforms(), equalTo(asList(Platform.ANDROID)));
        assertThat(actual.getTopUpAmount(), equalTo(TOP_UP_AMOUNT + 1));
        assertThat(actual.getValidForHours(), equalTo(100));
        assertThat(actual.getName(), equalTo("expected name"));
        assertThat(actual.getPromoType(), equalTo(PROMOTION_TYPE));
        assertThat(actual.getMaxRewards(), equalTo(4));
        assertThat(actual.isAllPlayers(), equalTo(true));
    }


    @Test
    public void updateShouldInsertPromotionRecordsForDailyAwardIfNonePreviouslyExisted() {

        final DailyAwardForm expected = getDefaultDailyAwardFormForSave();
        expected.setPromotionDefinitionId(null);
        expected.setName("expected name");
        expected.setCampaignId(CAMPAIGN_ID);
        expected.setValidForHours(100);
        expected.setPlatforms(asList(Platform.ANDROID));
        expected.setPromoType(PromotionType.DAILY_AWARD);
        expected.setTopUpAmount(TOP_UP_AMOUNT + 1);
        expected.setMaxRewards(23);

        underTest.update(expected);

        final DailyAwardForm actual = underTest.getForm(CAMPAIGN_ID);
        assertThat(actual.getPlatforms(), equalTo(asList(Platform.ANDROID)));
        assertThat(actual.getTopUpAmount(), equalTo(TOP_UP_AMOUNT + 1));
        assertThat(actual.getValidForHours(), equalTo(100));
        assertThat(actual.getName(), equalTo("expected name"));
        assertThat(actual.getPromoType(), equalTo(PROMOTION_TYPE));
        assertThat(actual.getMaxRewards(), equalTo(23));
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


    private DailyAwardForm getDefaultDailyAwardFormForSave() {

        DailyAwardForm dailyAwardForm = new DailyAwardForm();

        dailyAwardForm.setPromotionDefinitionId(null);
        dailyAwardForm.setName(PROMOTION_NAME);
        dailyAwardForm.setMaxRewards(MAX_REWARDS);
        dailyAwardForm.setValidForHours(VALID_FOR_HOURS);
        dailyAwardForm.setPriority(PRIORITY);
        dailyAwardForm.setPlatforms(PLATFORMS);
        dailyAwardForm.setCampaignId(CAMPAIGN_ID);
        dailyAwardForm.setPromoType(PROMOTION_TYPE);
        dailyAwardForm.setTopUpAmount(TOP_UP_AMOUNT);
        dailyAwardForm.setAllPlayers(true);

        return dailyAwardForm;
    }


    private DailyAwardForm fetchDailyAwardPromotionDefinitionByName(final String name) {
        final HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("name", name);

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
