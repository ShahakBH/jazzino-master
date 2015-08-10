package com.yazino.bi.campaign.dao;

import com.yazino.bi.campaign.domain.CampaignDefinition;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.yazino.engagement.campaign.domain.NotificationChannelConfigType.FILTER_OUT_120_DAY_UNOPENED;
import static com.yazino.engagement.campaign.domain.NotificationChannelConfigType.TEMPLATE;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional()
@DirtiesContext
public class MySqlCampaignDefinitionDaoIntegrationTest {
    private static final Long CAMPAIGN_ID = 1l;
    public static final String SEGMENT_SELECTION_QUERY = "select 1";
    private MySqlCampaignDefinitionDao underTest;

    private List<ChannelType> channelList;
    @Autowired
    @Qualifier("dwJdbcTemplate")
    private JdbcTemplate jdbc;
    private Map<NotificationChannelConfigType, String> channelConfig = newHashMap();

    @Before
    public void setUp() throws Exception {
        jdbc.update("DELETE FROM CAMPAIGN_CONTENT");
        jdbc.update("DELETE FROM CAMPAIGN_RUN");
        jdbc.update("DELETE FROM CAMPAIGN_CHANNEL");
        jdbc.update("DELETE FROM CAMPAIGN_CHANNEL_CONFIG");
        jdbc.update("DELETE FROM CAMPAIGN_TARGET");
        jdbc.update("DELETE FROM CAMPAIGN_DEFINITION");
        underTest = new MySqlCampaignDefinitionDao(jdbc);
        channelList = asList(
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST, ChannelType.IOS,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID);
    }

    @Test
    public void fetchCampaignShouldReturnCampaign() throws Exception {

        final HashMap<String, String> expectedContentMap = new HashMap<>();
        putContentValueInMap(expectedContentMap, "rap", getMessageForCampaign());

        final Map<NotificationChannelConfigType, String> channelConfig = newHashMap();
        final CampaignDefinition expectedCampaignDefinition =
                new CampaignDefinition(
                        CAMPAIGN_ID,
                        "name",
                        SEGMENT_SELECTION_QUERY,
                        expectedContentMap,
                        channelList,
                        Boolean.TRUE,
                        channelConfig,
                        true,
                        false);
        setupCampaignDefinitionRecord(expectedCampaignDefinition);

        final CampaignDefinition actualCampaignDefinition = underTest.fetchCampaign(CAMPAIGN_ID);
        assertThat(actualCampaignDefinition, equalTo(expectedCampaignDefinition));
    }

    @Test
    public void fetchCampaignShouldReturnDisabledCampaign() {
        final HashMap<String, String> expectedContentMap = new HashMap<>();
        final Map<NotificationChannelConfigType, String> channelConfig = newHashMap();
        final CampaignDefinition expectedCampaignDefinition =
                new CampaignDefinition(
                        CAMPAIGN_ID,
                        "name",
                        SEGMENT_SELECTION_QUERY,
                        expectedContentMap,
                        channelList,
                        Boolean.TRUE,
                        channelConfig,
                        false,
                        false);
        setupCampaignDefinitionRecord(expectedCampaignDefinition);
        assertThat(underTest.fetchCampaign(CAMPAIGN_ID).isEnabled(), is(false));
    }

    @Test
    public void saveDisabledCampaignShouldSaveCorrectly() {
        final HashMap<String, String> expectedContentMap = new HashMap<>();
        final Map<NotificationChannelConfigType, String> channelConfig = newHashMap();
        final CampaignDefinition expectedCampaignDefinition =
                new CampaignDefinition(
                        null,
                        "name",
                        SEGMENT_SELECTION_QUERY,
                        expectedContentMap,
                        channelList,
                        Boolean.TRUE,
                        channelConfig,
                        false,
                        false);
        final Long campaignId = underTest.save(expectedCampaignDefinition);
        final Boolean enabled = jdbc.queryForObject(
                format("select enabled from CAMPAIGN_DEFINITION WHERE ID=%s ", campaignId), Boolean.class);
        assertThat(enabled, is(equalTo(Boolean.FALSE)));
    }

    @Test
    public void saveDelayedCampaignShouldSaveAndLoadCorrectly() {
        final Map<String, String> expectedContentMap = of();
        CampaignDefinition campaignDefinition = new CampaignDefinition(
                null,
                "name",
                SEGMENT_SELECTION_QUERY,
                expectedContentMap,
                channelList,
                Boolean.TRUE,
                channelConfig,
                false,
                true
        );
        final Long id = underTest.save(campaignDefinition);
        final CampaignDefinition loadedCampaign = underTest.fetchCampaign(id);

        assertThat(loadedCampaign.delayNotifications(), is(true));

    }

    @Test
    public void fetchCampaignShouldReturnCampaignEvenIfChannelsNotPresent() throws Exception {

        final Map<NotificationChannelConfigType, String> channelConfig = newHashMap();
        final HashMap<String, String> expectedContentMap = new HashMap<>();
        putContentValueInMap(expectedContentMap, "rap", getMessageForCampaign());

        final CampaignDefinition expectedCampaignDefinition =
                new CampaignDefinition(
                        CAMPAIGN_ID,
                        "name",
                        SEGMENT_SELECTION_QUERY,
                        expectedContentMap,
                        new ArrayList<ChannelType>(),
                        Boolean.TRUE,
                        channelConfig,
                        true,
                        false);
        setupCampaignDefinitionRecord(expectedCampaignDefinition);

        final CampaignDefinition actualCampaignDefinition = underTest.fetchCampaign(CAMPAIGN_ID);
        assertThat(actualCampaignDefinition, equalTo(expectedCampaignDefinition));
    }

    private String getMessageForCampaign() {
        return "shizzle me nizzle zizzle "
                + "to fizzle wizzle "
                + "h to the izzle "
                + "I pizzle in the tizzle"
                + "my dog has a bizzle he returns when i wizzle";
    }

    @Test
    public void saveShouldInsertRecord() throws Exception {
        final HashMap<String, String> expectedContentMap = new HashMap<>();
        putContentValueInMap(expectedContentMap, "rap", getMessageForCampaign());


        final CampaignDefinition campaignDefinition = new CampaignDefinition(
                null,
                "name",
                SEGMENT_SELECTION_QUERY,
                expectedContentMap,
                channelList,
                Boolean.TRUE,
                channelConfig,
                true,
                false);
        final Long actualCampaignId = underTest.save(campaignDefinition);
        final CampaignDefinition actualCampaignDefinition = getCampaignFromDB(actualCampaignId);

        assertThat(actualCampaignDefinition.getName(), equalTo("name"));
        assertThat(actualCampaignDefinition.getContent().get("rap"), equalTo(getMessageForCampaign()));
        assertThat(actualCampaignDefinition.getChannels(), equalTo(channelList));
        assertThat(actualCampaignDefinition.getSegmentSelectionQuery(), equalTo(SEGMENT_SELECTION_QUERY));
        assertThat(actualCampaignDefinition.hasPromo(), equalTo(Boolean.TRUE));
    }

    @Test
    public void saveShouldNotTryToInsertChannelsIfNotPresent() throws Exception {
        final HashMap<String, String> expectedContentMap = new HashMap<>();

        final CampaignDefinition campaignDefinition = new CampaignDefinition(
                null,
                "name",
                SEGMENT_SELECTION_QUERY,
                expectedContentMap,
                null,
                Boolean.TRUE,
                channelConfig, true, false);
        final Long actualCampaignId = underTest.save(campaignDefinition);
        final CampaignDefinition actualCampaignDefinition = getCampaignFromDB(actualCampaignId);

        List<ChannelType> emptyChannelTypes = new ArrayList<>();
        assertThat(actualCampaignDefinition.getChannels(), is(emptyChannelTypes));
        assertThat(actualCampaignDefinition.hasPromo(), equalTo(Boolean.TRUE));
    }

    @Test
    public void updateShouldUpdateExistingRecord() {
        Map<NotificationChannelConfigType, String> newChannelConfig = newHashMap();
        newChannelConfig.put(NotificationChannelConfigType.TEMPLATE, "new Temmplate name");

        final HashMap<String, String> existingContent = new HashMap<>();
        putContentValueInMap(existingContent, "rap", getMessageForCampaign());

        final HashMap<String, String> expectedContent = new HashMap<>();
        putContentValueInMap(expectedContent, "rap", "a snap snap");
        putContentValueInMap(expectedContent, "bap", "a tap tap");

        final CampaignDefinition existingCampaignDefinition = new CampaignDefinition(
                null,
                "name",
                SEGMENT_SELECTION_QUERY,
                existingContent,
                channelList,
                Boolean.FALSE,
                channelConfig, true, false);
        final Long campaignId = underTest.save(existingCampaignDefinition);
        final CampaignDefinition updatedCampaignDefinition = new CampaignDefinition(
                campaignId,
                "updated name",
                "select 3",
                expectedContent,
                asList(ChannelType.IOS),
                Boolean.TRUE,
                newChannelConfig, true, false);

        underTest.update(updatedCampaignDefinition);

        Assert.assertThat(getCampaignFromDB(campaignId), is(IsEqual.equalTo(updatedCampaignDefinition)));
    }

    @Test
    public void testSaveShouldHandleMultipleContentItems() throws Exception {
        final HashMap<String, String> expectedContentMap = new HashMap<>();

        putContentValueInMap(expectedContentMap, "rap", getMessageForCampaign());
        putContentValueInMap(expectedContentMap, "short", "abcdefg");

        final CampaignDefinition campaignDefinition = new CampaignDefinition(
                null,
                "name",
                SEGMENT_SELECTION_QUERY,
                expectedContentMap,
                channelList,
                Boolean.FALSE,
                channelConfig, true, false);

        final Long actualCampaignId = underTest.save(campaignDefinition);
        final Map<String, String> actualContent = fetchContentMap(actualCampaignId);

        assertThat(actualContent.get("short"), equalTo("abcdefg"));
        assertThat(actualContent.get("rap"), equalTo(getMessageForCampaign()));

    }

    @Test
    public void getContentShouldReturnMapOfContentItems() {

        final Map<String, String> expectedContentMap = new HashMap<>();
        expectedContentMap.put("item1", "value1");
        expectedContentMap.put("item2", "value2");
        expectedContentMap.put("item3", "value3");

        setupCampaignDefinitionRecord(
                new CampaignDefinition(
                        CAMPAIGN_ID,
                        "getContentTest",
                        SEGMENT_SELECTION_QUERY,
                        expectedContentMap,
                        channelList,
                        Boolean.FALSE,
                        channelConfig, true, false));

        final Map<String, String> actualContent = underTest.getContent(CAMPAIGN_ID);
        Assert.assertThat(actualContent, equalTo(expectedContentMap));
    }

    @Test
    public void getContentShouldReturnEmptyMapIfNoContentItemsForCampaign() {
        final Map<String, String> expectedContentMap = new HashMap<>();

        setupCampaignDefinitionRecord(
                new CampaignDefinition(
                        CAMPAIGN_ID,
                        "getContentTest",
                        SEGMENT_SELECTION_QUERY,
                        null,
                        channelList,
                        Boolean.FALSE,
                        channelConfig, true, false));

        final Map<String, String> actualContent = underTest.getContent(CAMPAIGN_ID);
        Assert.assertThat(actualContent, equalTo(expectedContentMap));
    }

    @Test
    public void fetchCampaignShouldReturnChannelConfig() {
        final Long actualCampaignId = underTest.save(
                new CampaignDefinition(
                        CAMPAIGN_ID,
                        "getContentTest",
                        SEGMENT_SELECTION_QUERY,
                        new HashMap<String, String>(),
                        channelList,
                        Boolean.FALSE,
                        new HashMap<NotificationChannelConfigType, String>(), true, false));

        jdbc.execute(
                "insert into CAMPAIGN_CHANNEL_CONFIG VALUES(" + actualCampaignId + ",'" + FILTER_OUT_120_DAY_UNOPENED.toString() + "','TRUE')");
        jdbc.execute(
                "insert into CAMPAIGN_CHANNEL_CONFIG VALUES(" + actualCampaignId + ",'" + TEMPLATE.toString() + "','YOUR_MUM')");
        final CampaignDefinition campaignDefinition = underTest.fetchCampaign(actualCampaignId);
        assertThat(campaignDefinition.getChannelConfig().get(FILTER_OUT_120_DAY_UNOPENED), equalTo("TRUE"));
        assertThat(campaignDefinition.getChannelConfig().get(TEMPLATE), equalTo("YOUR_MUM"));
    }

    @Test
    public void saveCampaignShouldSaveContentConfig() {
        final HashMap<NotificationChannelConfigType, String> config = new HashMap<NotificationChannelConfigType, String>();
        config.put(TEMPLATE, "Your momma");
        config.put(FILTER_OUT_120_DAY_UNOPENED, "true");
        final Long actualCampaignId = underTest.save(
                new CampaignDefinition(
                        CAMPAIGN_ID,
                        "getContentTest",
                        SEGMENT_SELECTION_QUERY,
                        new HashMap<String, String>(),
                        channelList,
                        Boolean.FALSE,
                        config, true, false)
        );
        assertThat(
                jdbc.queryForObject(
                        "select CONFIG_VALUE FROM CAMPAIGN_CHANNEL_CONFIG WHERE CAMPAIGN_ID=" + actualCampaignId + " and CONFIG_KEY='" + TEMPLATE + "'",
                        String.class), equalTo("Your momma"));
        assertThat(
                jdbc.queryForObject(
                        "select CONFIG_VALUE FROM CAMPAIGN_CHANNEL_CONFIG WHERE CAMPAIGN_ID=" + actualCampaignId + " and CONFIG_KEY='" + FILTER_OUT_120_DAY_UNOPENED + "'",
                        String.class), equalTo("true"));
    }

    private Map<String, String> fetchContentMap(final Long actualCampaignId) {
        return jdbc.query(
                "SELECT CONTENT_KEY, CONTENT_VALUE FROM CAMPAIGN_CONTENT WHERE CAMPAIGN_ID = ?",
                new ResultSetExtractor<HashMap<String, String>>() {
                    @Override
                    public HashMap<String, String> extractData(
                            final ResultSet rs) throws SQLException, DataAccessException {
                        final HashMap<String, String> contentMap = new HashMap<>();
                        while (rs.next()) {
                            contentMap.put(rs.getString(1), rs.getString(2));
                        }
                        return contentMap;
                    }
                }, actualCampaignId
        );
    }

    private String putContentValueInMap(final HashMap<String, String> map,
                                        final String key,
                                        final String value) {
        return map.put(key, value);
    }

    private void setupCampaignDefinitionRecord(CampaignDefinition campaignDefinition) {
        jdbc.update(
                "INSERT INTO CAMPAIGN_DEFINITION (ID, name, segmentSqlQuery, hasPromo, enabled, delay_notifications) " +
                        "VALUES (?,?,?,?,?,?)",
                campaignDefinition.getId(),
                campaignDefinition.getName(),
                campaignDefinition.getSegmentSelectionQuery(),
                campaignDefinition.hasPromo(),
                campaignDefinition.isEnabled(),
                campaignDefinition.delayNotifications()
        );

        Map<ChannelType, Integer> channelTypes = getChannelTypeFromDb();

        List<ChannelType> channels = campaignDefinition.getChannels();
        if (channels != null && !channels.isEmpty()) {
            for (ChannelType channel : channels) {
                jdbc.update(
                        "INSERT INTO CAMPAIGN_CHANNEL (CAMPAIGN_ID, CHANNEL_ID) VALUES (?,?)",
                        campaignDefinition.getId(),
                        channelTypes.get(channel));
            }
        }

        if (campaignDefinition.getContent() != null) {
            for (Map.Entry<String, String> entry : campaignDefinition.getContent().entrySet()) {
                jdbc.update(
                        "INSERT INTO CAMPAIGN_CONTENT (CAMPAIGN_ID, CONTENT_KEY, CONTENT_VALUE) VALUES (?,?,?)",
                        campaignDefinition.getId(), entry.getKey(), entry.getValue());
            }
        }
    }

    private Map<ChannelType, Integer> getChannelTypeFromDb() {
        return jdbc.query(
                "select ID, CHANNEL_NAME FROM CHANNEL_TYPE ORDER BY ID",
                new ResultSetExtractor<Map<ChannelType, Integer>>() {
                    @Override
                    public Map<ChannelType, Integer> extractData(
                            final ResultSet rs) throws SQLException, DataAccessException {
                        Map<ChannelType, Integer> channels = newLinkedHashMap();
                        while (rs.next()) {
                            channels.put(ChannelType.valueOf(rs.getString("CHANNEL_NAME")), rs.getInt("ID"));
                        }
                        return channels;
                    }
                });
    }

    private List<ChannelType> getChannelTypeForCampaign(final Long campaignId) {
        try {
            return jdbc.query(
                    "select CHANNEL_NAME FROM CAMPAIGN_CHANNEL cc JOIN CHANNEL_TYPE ct on cc.channel_id = ct.id "
                            + "WHERE CAMPAIGN_ID = ? ORDER BY ID", new RowMapper<ChannelType>() {
                        @Override
                        public ChannelType mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                            return ChannelType.valueOf(rs.getString("CHANNEL_NAME"));
                        }
                    }, campaignId);
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }

    private Map<NotificationChannelConfigType, String> getChannelConfigForCampaign(final Long campaignId) {

        try {
            final Map<NotificationChannelConfigType, String> result = newHashMap();
            jdbc.query(
                    "select * FROM CAMPAIGN_CHANNEL_CONFIG "
                            + "WHERE CAMPAIGN_ID = ? ORDER BY CAMPAIGN_ID", new RowMapper() {
                        @Override
                        public Object mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                            result.put(
                                    NotificationChannelConfigType.valueOf(rs.getString("CONFIG_KEY")),
                                    rs.getString("CONFIG_VALUE"));

                            return null;
                        }
                    }, campaignId);
            return result;
        } catch (EmptyResultDataAccessException e) {
            return newHashMap();
        }
    }

    private CampaignDefinition getCampaignFromDB(final Long campaignId) {
        final Map<String, String> contentMap = fetchContentMap(campaignId);
        final List<ChannelType> channelTypesForCampaign = getChannelTypeForCampaign(campaignId);
        final Map<NotificationChannelConfigType, String> loadedChannelConfig = getChannelConfigForCampaign(campaignId);

        return jdbc.queryForObject(
                "select ID, NAME, segmentSqlQuery, hasPromo from CAMPAIGN_DEFINITION Where id= ?",
                new RowMapper<CampaignDefinition>() {
                    @Override
                    public CampaignDefinition mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return new CampaignDefinition(
                                rs.getLong("ID"),
                                rs.getString("NAME"),
                                rs.getString("segmentSqlQuery"),
                                contentMap,
                                channelTypesForCampaign,
                                rs.getBoolean("hasPromo"),
                                loadedChannelConfig,
                                true,
                                false);
                    }
                }, campaignId
        );
    }
}
