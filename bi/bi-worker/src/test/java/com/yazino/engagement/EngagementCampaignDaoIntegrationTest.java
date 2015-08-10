package com.yazino.engagement;

import com.yazino.engagement.campaign.AppRequestExternalReference;
import com.yazino.engagement.campaign.dao.EngagementCampaignDao;
import com.yazino.engagement.facebook.FacebookAppRequestEnvelope;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
@TransactionConfiguration
@Transactional
public class EngagementCampaignDaoIntegrationTest {

    public static final String FACEBOOK1_ID = "1001";
    public static final Integer TARGET_ID = 2001;
    public static final Integer UNKNOWN_TARGET_ID = 452436;
    public static final int ENGAGEMENT_CAMPAIGN_ID = 56;
    public static final String MESSAGE = "a simple message";
    public static final String TRACKING_DATA = "tracking data";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final int PLAYER1_ID = 1111111;

    @Before
    public void setup() {
        underTest = new EngagementCampaignDao(jdbcTemplate);

        jdbcTemplate.execute("delete from APP_REQUEST");
        jdbcTemplate.execute("delete from APP_REQUEST_TARGET");
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2012, 11, 12, 13, 59, 30, 0).getMillis());

        jdbcTemplate.update(
                "INSERT INTO APP_REQUEST (ID, CHANNEL_TYPE, TITLE, DESCRIPTION, MESSAGE, CREATED, TARGET_COUNT, TRACKING)"
                        + " values(?,?,?,?,?,?,?,?)", ENGAGEMENT_CAMPAIGN_ID, "target client", TITLE, DESCRIPTION, MESSAGE,
                new Date(), 3, TRACKING_DATA);

        jdbcTemplate.update(
                "INSERT INTO APP_REQUEST_TARGET (ID, APP_REQUEST_ID, GAME_TYPE, PLAYER_ID, EXTERNAL_ID)"
                        + " values(?,?,?,?,?)", TARGET_ID, ENGAGEMENT_CAMPAIGN_ID, SLOTS, PLAYER1_ID, FACEBOOK1_ID);
    }
    public static final String SLOTS = "SLOTS";

    public static final String BLACKJACK = "BLACKJACK";
    @Autowired
    @Qualifier("dwJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private EngagementCampaignDao underTest;

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new EngagementCampaignDao(mockTemplate);
        when(mockTemplate.queryForObject(anyString(), Mockito.<RowMapper>any(), anyString(),
                anyString())).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.fetchAppRequestEnvelopeByCampaignAndTargetId(ENGAGEMENT_CAMPAIGN_ID, TARGET_ID);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = CannotAcquireLockException.class)
    public void transientDatabaseProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new EngagementCampaignDao(mockTemplate);
        when(mockTemplate.queryForObject(anyString(), Mockito.<RowMapper>any(), anyString(),
                anyString())).thenThrow(
                new CannotAcquireLockException("aTestException", new SQLException()));

        underTest.fetchAppRequestEnvelopeByCampaignAndTargetId(ENGAGEMENT_CAMPAIGN_ID, TARGET_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void exceptionsThatAreNotConnectionOrTransientProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new EngagementCampaignDao(mockTemplate);
        when(mockTemplate.queryForObject(anyString(), Mockito.<RowMapper>any(), anyString(),
                anyString())).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.fetchAppRequestEnvelopeByCampaignAndTargetId(ENGAGEMENT_CAMPAIGN_ID, TARGET_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    // belt and braces test, consumer expects a single message. Should the database be fucked, i.e. multiple rows
    // with same target id, then we should catch the exception, log it and ignore result
    public void whenQueryReturnsMoreThanOneMessageThenExceptionIsNotPropagatedAndNullIsReturned() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new EngagementCampaignDao(mockTemplate);
        when(mockTemplate.queryForObject(anyString(), Mockito.<RowMapper>any(), anyString(),
                anyString())).thenThrow(
                new IncorrectResultSizeDataAccessException("too many", 1, 2));

        underTest.fetchAppRequestEnvelopeByCampaignAndTargetId(ENGAGEMENT_CAMPAIGN_ID, TARGET_ID);
    }

    @Test
    public void shouldReturnNullWhenNoMessageExistsForUser() {
        final FacebookAppRequestEnvelope facebookAppRequestEnvelope = underTest.fetchAppRequestEnvelopeByCampaignAndTargetId(
                ENGAGEMENT_CAMPAIGN_ID,
                UNKNOWN_TARGET_ID);
        assertNull(facebookAppRequestEnvelope);
    }

    @Test
    public void shouldReturnAppRequestMessageForTarget() {
        final FacebookAppRequestEnvelope facebookAppRequestEnvelope = underTest.fetchAppRequestEnvelopeByCampaignAndTargetId(
                ENGAGEMENT_CAMPAIGN_ID, TARGET_ID);
        assertThat(facebookAppRequestEnvelope, is(new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK1_ID, SLOTS, MESSAGE, TRACKING_DATA, null)));
    }

    @Test
    @Transactional
    public void shouldSaveExternalReferenceForTarget() {
        underTest.saveExternalReference(TARGET_ID, "some external reference");
        final Map<String, Object> result = jdbcTemplate.queryForMap("SELECT * FROM APP_REQUEST_TARGET WHERE ID=?", TARGET_ID);
        Assert.assertEquals("some external reference", result.get("EXTERNAL_REF"));
    }

    @Test
    public void shouldRetrieveAllAppRequestExternalReferences() {
        jdbcTemplate.update(
                "INSERT INTO APP_REQUEST_TARGET (ID, APP_REQUEST_ID, GAME_TYPE, PLAYER_ID, EXTERNAL_ID, EXTERNAL_REF)"
                        + " values(?,?,?,?,?,?)", TARGET_ID + 1, ENGAGEMENT_CAMPAIGN_ID, SLOTS, PLAYER1_ID + 1, "fbId-1", "ref1");
        jdbcTemplate.update(
                "INSERT INTO APP_REQUEST_TARGET (ID, APP_REQUEST_ID, GAME_TYPE, PLAYER_ID, EXTERNAL_ID, EXTERNAL_REF)"
                        + " values(?,?,?,?,?,?)", TARGET_ID + 2, ENGAGEMENT_CAMPAIGN_ID, BLACKJACK, PLAYER1_ID + 2, "fbId-2", "ref2");
        final List<AppRequestExternalReference> result = underTest.fetchAppRequestExternalReferences(ENGAGEMENT_CAMPAIGN_ID);
        assertThat(result, hasItem(new AppRequestExternalReference("fbId-1", SLOTS, "ref1")));
        assertThat(result, hasItem(new AppRequestExternalReference("fbId-2", BLACKJACK, "ref2")));
    }

    @Test
    public void shouldRetrieveAllAppRequestsThatHaveExpiredAndBeenSent() {
        int id = 5678;

        jdbcTemplate.update(
                "INSERT INTO APP_REQUEST (ID, CHANNEL_TYPE, TITLE, DESCRIPTION, MESSAGE, CREATED, TARGET_COUNT, STATUS, TRACKING, EXPIRY_DT)"
                        + " values(?,?,?,?,?,?,?,?,?,?)", id, ChannelType.FACEBOOK_APP_TO_USER_REQUEST.name(), "title", "desc", MESSAGE,
                new Date(), 3, 2, TRACKING_DATA, new DateTime(2010, 11, 12, 13, 59, 30, 0).toDate());

        final List<Integer> result = underTest.fetchCampaignsToExpire();
        assertThat(result, hasItem(id));

    }

    @Test
    public void shouldIgnoreExpiredCampaignsWhereRequestsCannotBeDeleted() {
        int id = 5678;
        jdbcTemplate.update(
                "INSERT INTO APP_REQUEST (ID, CHANNEL_TYPE, TITLE, DESCRIPTION, MESSAGE, CREATED, TARGET_COUNT, STATUS, TRACKING, EXPIRY_DT)"
                        + " values(?,?,?,?,?,?,?,?,?,?)", id, ChannelType.IOS.name(), "title", "desc", MESSAGE,
                new Date(), 3, 2, TRACKING_DATA, new DateTime(2010, 11, 12, 13, 59, 30, 0).toDate());
        Assert.assertFalse(ChannelType.IOS.canDeleteRequests());

        final List<Integer> result = underTest.fetchCampaignsToExpire();
        assertTrue(result.isEmpty());

    }

    @Test
    public void shouldSaveStatusforAppRequest() {
        int id = 2678;
        jdbcTemplate.update(
                "INSERT INTO APP_REQUEST (ID, CHANNEL_TYPE, TITLE, DESCRIPTION, MESSAGE, CREATED, TARGET_COUNT, STATUS, TRACKING, EXPIRY_DT)"
                        + " values(?,?,?,?,?,?,?,?,?,?)", id, "target client", "title", "desc", MESSAGE,
                new Date(), 3, 2, TRACKING_DATA, new DateTime(2010, 11, 12, 13, 59, 30, 0).toDate());

        underTest.updateCampaignStatus(id, EngagementCampaignStatus.EXPIRED);

        final int result = jdbcTemplate.queryForInt("SELECT STATUS FROM APP_REQUEST WHERE ID=?", id);
        assertThat(EngagementCampaignStatus.EXPIRED.getValue(), is(result));
    }

    @Test
    public void shouldUpdateStatusToExpiringAndReturnTrue(){
        // given a campaign in the sent state with expiry time in the past
        int campaignId = 23;
        jdbcTemplate.update(
                "INSERT INTO APP_REQUEST (ID, CHANNEL_TYPE, TITLE, DESCRIPTION, MESSAGE, CREATED, TARGET_COUNT, STATUS, TRACKING, EXPIRY_DT)"
                        + " values(?,?,?,?,?,?,?,?,?,?)", campaignId, "", "", "", "",
                new Date(), 3, EngagementCampaignStatus.SENT.getValue(), "", new DateTime(2010, 11, 12, 13, 59, 30, 0).toDate());

        final boolean expiring = underTest.updateCampaignStatusToExpiring(campaignId);

        assertTrue(expiring);
        final int actualStatus = jdbcTemplate.queryForInt("select status from APP_REQUEST where id=" + campaignId);
        assertThat(EngagementCampaignStatus.EXPIRING.getValue(), is(actualStatus));
    }

    @Test
    public void shouldFailToUpdateStatusToExpiringWhenCurrentStatusIsExpiring(){
        // given a campaign in the expiring state with expiry time in the past
        int campaignId = 23;
        jdbcTemplate.update(
                "INSERT INTO APP_REQUEST (ID, CHANNEL_TYPE, TITLE, DESCRIPTION, MESSAGE, CREATED, TARGET_COUNT, STATUS, TRACKING, EXPIRY_DT)"
                        + " values(?,?,?,?,?,?,?,?,?,?)", campaignId, "", "", "", "",
                new Date(), 3, EngagementCampaignStatus.EXPIRING.getValue(), "", new DateTime(2010, 11, 12, 13, 59, 30, 0).toDate());

        final boolean expiring = underTest.updateCampaignStatusToExpiring(campaignId);

        Assert.assertFalse(expiring);
        final int actualStatus = jdbcTemplate.queryForInt("select status from APP_REQUEST where id=" + campaignId);
        assertThat(EngagementCampaignStatus.EXPIRING.getValue(), is(actualStatus));
    }

    @Test
    public void shouldFailToUpdateStatusToExpiringWhenCampaignHasNotExpired(){
        // given a campaign in the sent state with expiry time in the future
        int campaignId = 23;
        jdbcTemplate.update(
                "INSERT INTO APP_REQUEST (ID, CHANNEL_TYPE, TITLE, DESCRIPTION, MESSAGE, CREATED, TARGET_COUNT, STATUS, TRACKING, EXPIRY_DT)"
                        + " values(?,?,?,?,?,?,?,?,?,?)", campaignId, "", "", "", "",
                new Date(), 3, EngagementCampaignStatus.SENT.getValue(), "", new DateTime(2060, 11, 12, 13, 59, 30, 0).toDate());

        final boolean expiring = underTest.updateCampaignStatusToExpiring(campaignId);

        Assert.assertFalse(expiring);
        final int actualStatus = jdbcTemplate.queryForInt("select status from APP_REQUEST where id=" + campaignId);
        assertThat(EngagementCampaignStatus.SENT.getValue(), is(actualStatus));
    }
}
