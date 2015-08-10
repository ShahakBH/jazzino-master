package com.yazino.bi.operations.engagement;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.EngagementCampaignStatus;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.util.ObjectUtils.nullSafeEquals;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
@SuppressWarnings("NullableProblems")
@Transactional
public class EngagementCampaignDaoIntegrationTest {

    public static final int ANDROID1_ID = 67;
    public static final int UNKNOWN_REQUEST_ID = -8888;
    public static final int FACEBOOK_ENGAGAGEMENT_CAMPAIGN_ID = 7;
    public static final int ANDROID_ENGAGEMENT_CAMPAIGN_ID2 = 69;
    public static final int ANDROID_CAMPAIGN1_INDEX = 1;
    public static final int ANDROID_CAMPAIGN2_INDEX = 2;
    public static final int IOS_CAMPAIGN1_INDEX = 0;
    public static final int IOS_CAMPAIGN2_INDEX = 3;
    public static final int IOS_CAMPAIGN3_INDEX = 4;
    public static final String SLOTS = "slots";
    public static final String PROVIDER_FACEBOOK = "FACEBOOK";
    public static final String PROVIDER_YAZINO = "YAZINO";

    @Autowired
    @Qualifier("replicaJdbcTemplate")
    private JdbcTemplate strataprod;

    @Autowired
    @Qualifier("dwJdbcTemplate")
    private JdbcTemplate strataproddw;

    @Autowired
    private EngagementCampaignDao underTest;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private EngagementCampaign[] existingEngagementCampaigns;
    private AppRequestTarget[] existingAppRequestTargetsForAndroid;

    @Before
    public void populate() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2012, 11, 12, 13, 59, 30, 0).getMillis());
        deleteTestData();
        setUpEngagementCampaigns();
        insertAppRequests(existingEngagementCampaigns);
        createAppRequestTargets();
    }

    @After
    public void tearDown() {
        resetJoda();
    }

    private void deleteTestData() {
        strataproddw.execute("delete from APP_REQUEST_TARGET");
        strataproddw.execute("delete from APP_REQUEST");
        strataprod.execute("delete from GIFTS");
        strataprod.execute("delete from YAZINO_LOGIN where PLAYER_ID");
        strataprod.execute("delete from LOBBY_USER where PLAYER_ID");
        strataproddw.execute("delete from MARKETING_GROUP_MEMBER where MARKETING_GROUP_ID");
        strataproddw.execute("delete from MARKETING_GROUP where ID");
    }

    private void resetJoda() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    private void insertAppRequests(EngagementCampaign... requests) {
        for (EngagementCampaign engagementCampaign : requests) {
            strataproddw.update(
                    "INSERT INTO APP_REQUEST (ID, CHANNEL_TYPE, TITLE, DESCRIPTION, MESSAGE, TRACKING, CREATED, SENT, TARGET_COUNT, STATUS, SCHEDULED_DT, " +
                            "EXPIRY_DT)"
                            + " values(?,?,?,?,?,?,?,?,?,?,?,?)", engagementCampaign.getId(),
                    engagementCampaign.getChannelType().name(), engagementCampaign.getTitle(), engagementCampaign.getDescription(),
                    engagementCampaign.getMessage(), engagementCampaign.getTrackingReference(), formatDateTime(engagementCampaign.getCreated()),
                    formatDateTime(engagementCampaign.getSent()), engagementCampaign.getTargetCount(),
                    engagementCampaign.getStatus().getValue(),
                    formatDateTime(engagementCampaign.getScheduled()),
                    formatDateTime(engagementCampaign.getExpires()));
        }
    }

    private String formatDateTime(DateTime dateTime) {
        if (dateTime != null) {
            return DATE_TIME_FORMATTER.print(dateTime);
        }
        return null;
    }

    @Test
    @Transactional
    public void findByIdShouldReturnNullWhenIdIsUnknown() throws Exception {
        EngagementCampaign request = underTest.findById(UNKNOWN_REQUEST_ID);
        assertNull(request);
    }

    @Test
    @Transactional
    public void findByIdShouldReturnCorrectRequest() throws Exception {
        EngagementCampaign request = underTest.findById(ANDROID1_ID);
        assertThat(request, is(existingEngagementCampaigns[ANDROID_CAMPAIGN1_INDEX]));
    }

    @Test
    @Transactional
    public void findAllShouldReturnAnEmptyListWhenNoRequestsExists() throws Exception {
        final List<EngagementCampaign> engagementCampaigns = underTest.findAll(ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION);
        assertTrue(engagementCampaigns.isEmpty());
    }

    @Test
    @Transactional
    public void findAllAndroidRequestsShouldOnlyReturnAndroidRequests() throws Exception {
        final List<EngagementCampaign> engagementCampaigns = underTest.findAll(ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID);

        assertThat(engagementCampaigns.size(), is(2));
        assertThat(engagementCampaigns, hasItem(existingEngagementCampaigns[ANDROID_CAMPAIGN1_INDEX]));
        assertThat(engagementCampaigns, hasItem(existingEngagementCampaigns[ANDROID_CAMPAIGN2_INDEX]));
    }

    @Test
    @Transactional
    public void findAllIosRequestsShouldOnlyReturnIosRequests() throws Exception {
        final List<EngagementCampaign> engagementCampaigns = underTest.findAll(ChannelType.IOS);

        assertThat(engagementCampaigns.size(), is(3));
        assertThat(engagementCampaigns, hasItems(existingEngagementCampaigns[IOS_CAMPAIGN1_INDEX], existingEngagementCampaigns[IOS_CAMPAIGN2_INDEX], existingEngagementCampaigns[IOS_CAMPAIGN3_INDEX]));
    }


    @Test
    @Transactional
    public void findAllByStatusShouldReturnIOSCreatedRequests() throws Exception {
        final List<EngagementCampaign> createdEngagementCampaigns = underTest.findAllByStatus(ChannelType.IOS,
                EngagementCampaignStatus.CREATED);

        assertThat(createdEngagementCampaigns.size(), is(1));
        assertThat(createdEngagementCampaigns, hasItem(existingEngagementCampaigns[0]));
    }

    @Test
    @Transactional
    public void findAllByStatusShouldReturnIOSSentRequests() throws Exception {
        final List<EngagementCampaign> sentEngagementCampaigns = underTest.findAllByStatus(ChannelType.IOS, EngagementCampaignStatus.SENT);

        assertThat(sentEngagementCampaigns.size(), is(2));
        assertThat(sentEngagementCampaigns, hasItems(existingEngagementCampaigns[IOS_CAMPAIGN2_INDEX], existingEngagementCampaigns[IOS_CAMPAIGN3_INDEX]));
    }

    @Test
    @Transactional
    public void findAllByStatusShouldReturnIOSProcessingRequests() throws Exception {
        final List<EngagementCampaign> createdEngagementCampaigns = underTest.findAllByStatus(ChannelType.IOS,
                EngagementCampaignStatus.PROCESSING);

        assertThat(createdEngagementCampaigns.size(), is(0));
    }

    @Test
    @Transactional
    public void createShouldCreateNewRequest() throws Exception {
        EngagementCampaign toCreate = new EngagementCampaignBuilder()
                .withChannelType(ChannelType.FACEBOOK_APP_TO_USER_REQUEST)
                .withTitle("new")
                .withDescription("new DESC")
                .withMessage("new Come back!")
                .withStatus(EngagementCampaignStatus.CREATED)
                .withTrackingReference("some tracking data")
                .withScheduled(new DateTime().plusHours(12))
                .withExpires(new DateTime().plusHours(56))
                .build();

        underTest.create(toCreate);

        // get the generated request id
        final int requestId = strataproddw.queryForInt("select last_insert_id()");

        EngagementCampaign expected = new EngagementCampaignBuilder(toCreate)
                .withId(requestId)
                .withCreateDate(new DateTime())
                .build();

        final EngagementCampaign actualRequest = underTest.findById(requestId);
        assertThat(actualRequest, is(expected));
    }

    @Test
    @Transactional
    public void createShouldReturnNewRequestId() throws Exception {
        EngagementCampaign toCreate = new EngagementCampaignBuilder()
                .withChannelType(ChannelType.FACEBOOK_APP_TO_USER_REQUEST)
                .withTitle("new")
                .withDescription("new DESC")
                .withMessage("new Come back!")
                .withStatus(EngagementCampaignStatus.CREATED)
                .withTrackingReference("some tracking data")
                .withScheduled(new DateTime().plusHours(12))
                .withExpires(new DateTime().plusHours(56))
                .build();

        int appRequestId = underTest.create(toCreate);

        int requestId = strataproddw.queryForInt("select last_insert_id()");
        assertThat(appRequestId, is(requestId));
    }

    @Test
    @Transactional
    public void createShouldCreateWithNullTracking() throws Exception {
        EngagementCampaign toCreate = new EngagementCampaignBuilder()
                .withChannelType(ChannelType.FACEBOOK_APP_TO_USER_REQUEST)
                .withTitle("new")
                .withDescription("new DESC")
                .withMessage("new Come back!")
                .withStatus(EngagementCampaignStatus.CREATED)
                .build();

        underTest.create(toCreate);

        // get the generated request id
        final int requestId = strataproddw.queryForInt("select last_insert_id()");
        EngagementCampaign expected = new EngagementCampaignBuilder(toCreate)
                .withId(requestId)
                .withCreateDate(new DateTime())
                .build();

        final EngagementCampaign actualRequest = underTest.findById(requestId);
        assertThat(actualRequest, is(expected));
    }

    @Test
    @Transactional
    public void updateShouldUpdateRequest() throws Exception {
        EngagementCampaign toUpdate = new EngagementCampaignBuilder(existingEngagementCampaigns[1])
                .withTitle("updated Title")
                .withDescription("updated desc")
                .withMessage("update message")
                .withTrackingReference("updated tracking data")
                .withStatus(EngagementCampaignStatus.PROCESSING)
                .withSentDate(new DateTime().plusDays(23))
                .withScheduled(new DateTime().plusDays(230))
                .build();

        underTest.update(toUpdate);

        final EngagementCampaign updated = underTest.findById(toUpdate.getId());
        assertThat(updated, is(toUpdate));
    }

    @Test
    @Transactional
    public void updateShouldUpdateWithNullSent() throws Exception {
        EngagementCampaign toUpdate = new EngagementCampaignBuilder(existingEngagementCampaigns[1])
                .withTitle("updated Title")
                .withDescription("updated desc")
                .withMessage("update message")
                .withTrackingReference("updated tracking data")
                .withStatus(EngagementCampaignStatus.PROCESSING)
                .withSentDate(null)
                .withScheduled(new DateTime().plusDays(1))
                .build();

        underTest.update(toUpdate);

        final EngagementCampaign updated = underTest.findById(toUpdate.getId());
        assertThat(updated, is(toUpdate));
    }

    @Test
    @Transactional
    public void updateShouldUpdateWithNullScheduled() throws Exception {
        EngagementCampaign toUpdate = new EngagementCampaignBuilder(existingEngagementCampaigns[1])
                .withTitle("updated Title")
                .withDescription("updated desc")
                .withMessage("update message")
                .withTrackingReference("updated tracking data")
                .withStatus(EngagementCampaignStatus.PROCESSING)
                .withScheduled(null)
                .build();

        underTest.update(toUpdate);

        final EngagementCampaign updated = underTest.findById(toUpdate.getId());
        assertThat(updated, is(toUpdate));
    }

    @Test
    @Transactional
    public void updateShouldUpdateWithNullExpires() throws Exception {
        EngagementCampaign toUpdate = new EngagementCampaignBuilder(existingEngagementCampaigns[1])
                .withTitle("updated Title")
                .withDescription("updated desc")
                .withMessage("update message")
                .withTrackingReference("updated tracking data")
                .withStatus(EngagementCampaignStatus.PROCESSING)
                .withExpires(null)
                .build();

        underTest.update(toUpdate);

        final EngagementCampaign updated = underTest.findById(toUpdate.getId());
        assertThat(updated, is(toUpdate));
    }

    @Test
    @Transactional
    public void deleteShouldHandleUnknownRequestId() {
        underTest.delete(UNKNOWN_REQUEST_ID);
    }

    @Test
    @Transactional
    public void deleteShouldDeleteRequestAndTargets() {
        underTest.delete(ANDROID1_ID);

        final EngagementCampaign androidRequest = underTest.findById(ANDROID1_ID);
        assertNull(androidRequest);

        final int androidRequestTargetCount = underTest.getTargetCountById(ANDROID1_ID);
        assertThat(androidRequestTargetCount, is(0));

    }

    @Test
    @Transactional
    public void getTargetCountByIdForUnknownRequestShouldReturnZero() throws Exception {
        int actualTargetCount = underTest.getTargetCountById(UNKNOWN_REQUEST_ID);
        assertThat(actualTargetCount, is(0));
    }

    @Test
    @Transactional
    public void getTargetCountByIdShouldReturnCorrectCount() throws Exception {
        int actualTargetCount = underTest.getTargetCountById(ANDROID1_ID);
        assertThat(actualTargetCount, is(existingAppRequestTargetsForAndroid.length));
    }

    @Test
    @Transactional
    public void findAppRequestTargetsByIdShouldReturnAllTargets() throws Exception {
        List<AppRequestTarget> actualTargets = underTest.findAppRequestTargetsById(ANDROID1_ID, 0, Integer.MAX_VALUE);

        assertThat(actualTargets.size(), is(existingAppRequestTargetsForAndroid.length));
        assertThat(actualTargets, hasItems(existingAppRequestTargetsForAndroid));
    }

    @Test
    @Transactional
    public void findAppRequestTargetsByIdShouldReturnBLACKJACKTarget() throws Exception {
        List<AppRequestTarget> actualTargets = underTest.findAppRequestTargetsById(ANDROID1_ID, 0, 2);

        assertThat(actualTargets.size(), is(2));
        assertThat(actualTargets, hasItems(existingAppRequestTargetsForAndroid[1],
                existingAppRequestTargetsForAndroid[2]));
    }

    @Test
    @Transactional
    public void findAppRequestTargetsByIdShouldNoTargetsWhenOffsetEqualToNumberOfTargets() throws Exception {
        List<AppRequestTarget> actualTargets = underTest.findAppRequestTargetsById(ANDROID1_ID,
                existingAppRequestTargetsForAndroid.length, Integer.MAX_VALUE);

        assertThat(actualTargets.size(), is(0));
    }

    @Test
    @Transactional
    public void addTargetsShouldAddNewTargetsAndIgnoreDuplicates() throws Exception {
        // given a new target and a current one
        AppRequestTarget newTarget = new AppRequestTargetBuilder()
                .withAppRequestId(ANDROID1_ID)
                .withPlayerId(BigDecimal.valueOf(666))
                .withExternalId("external id")
                .withGameType("SLOTS")
                .build();
        List<AppRequestTarget> targetsToAdd = asList(newTarget, existingAppRequestTargetsForAndroid[2]);

        underTest.addAppRequestTargets(ANDROID1_ID, targetsToAdd);

        final int newAppRequestTargetId = strataproddw.queryForInt("select last_insert_id()");
        newTarget.setId(newAppRequestTargetId);
        final List<AppRequestTarget> targetList = underTest.findAppRequestTargetsById(ANDROID1_ID, 0,
                Integer.MAX_VALUE);
        assertThat(targetList.size(), is(existingAppRequestTargetsForAndroid.length + 1));
        assertThat(targetList, hasItems(existingAppRequestTargetsForAndroid));
        assertThat(targetList, hasItems(newTarget));
    }

    @Test
    @Transactional
    public void addTargetsShouldUpdateTargetCountOfAppRequest() throws Exception {
        AppRequestTarget newTarget = new AppRequestTargetBuilder()
                .withAppRequestId(ANDROID1_ID)
                .withPlayerId(BigDecimal.valueOf(666))
                .withExternalId("external id")
                .withGameType("SLOTS")
                .build();
        List<AppRequestTarget> targetsToAdd = asList(newTarget);

        underTest.addAppRequestTargets(ANDROID1_ID, targetsToAdd);

        final EngagementCampaign androidTarget = underTest.findById(ANDROID1_ID);
        assertThat(androidTarget.getTargetCount(), is(existingAppRequestTargetsForAndroid.length + 1));
    }

    @Test
    @Transactional
    public void shouldReturnDueAppRequests() {
        // given a bunch of app requests in various scheduled states
        strataproddw.execute("delete from APP_REQUEST");
        int id = 1;
        final EngagementCampaign baseRequest = new EngagementCampaignBuilder().withChannelType(ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID)
                .withTitle("title")
                .withDescription("desc")
                .withMessage("message")
                .withCreateDate(new DateTime())
                .build();
        final EngagementCampaign notScheduled = new EngagementCampaignBuilder(baseRequest).withId(id++)
                .withScheduled(null)
                .withStatus(EngagementCampaignStatus.CREATED)
                .build();
        final EngagementCampaign scheduledButSent = new EngagementCampaignBuilder(baseRequest).withId(id++)
                .withScheduled(null)
                .withStatus(EngagementCampaignStatus.SENT)
                .withScheduled(new DateTime().minusHours(1))
                .build();
        final EngagementCampaign scheduledButProcessing = new EngagementCampaignBuilder(baseRequest).withId(id++)
                .withChannelType(ChannelType.IOS)
                .withScheduled(null)
                .withStatus(EngagementCampaignStatus.PROCESSING)
                .withScheduled(new DateTime().minusHours(23))
                .build();
        final EngagementCampaign scheduledNow = new EngagementCampaignBuilder(baseRequest).withScheduled(null).withId(id++)
                .withStatus(EngagementCampaignStatus.CREATED)
                .withScheduled(new DateTime().minusHours(23))
                .build();
        final EngagementCampaign scheduledInFuture = new EngagementCampaignBuilder(baseRequest).withId(id)
                .withScheduled(
                        null)
                .withStatus(EngagementCampaignStatus.CREATED)
                .withScheduled(new DateTime().plusSeconds(1))
                .build();
        insertAppRequests(notScheduled, scheduledButSent, scheduledButProcessing, scheduledInFuture, scheduledNow);

        // when getting due requests
        final List<EngagementCampaign> dueEngagementCampaigns = underTest.findDueEngagementCampaigns(new DateTime());

        // then should only have requests that have scheduled date in past and status of created
        assertThat(dueEngagementCampaigns.size(), is(1));
        assertThat(dueEngagementCampaigns, hasItems(scheduledNow));
    }

    /*
        for each channel create request,
         create multiple requests for player if using multiple channels
         don't create requests for unused channels
     */

    private void setupMarketingGroupMembers(final int marketingGroupId, final String gameType, final BigDecimal... playerIds) {
        insertMarketingGroup(marketingGroupId, "Reactivation Group");
        for (BigDecimal playerId : playerIds) {
            insertMarketingGroupMember(marketingGroupId, playerId, gameType);
        }
    }

    private AppRequestTarget appRequestTarget(Integer appRequestId, String gameType, BigDecimal playerId, String externalId) {
        return new AppRequestTarget(null, appRequestId, playerId, externalId, gameType);
    }

    private Integer appRequestId(Integer engagementCampaignId) {
        return engagementCampaignId;
    }

    private Matcher<AppRequestTarget> equivalentTo(AppRequestTarget expectedTarget) {
        return new EquivalentAppRequestTargetMatcher(expectedTarget);
    }

    private void insertLobbyUser(BigDecimal playerId, String externalId, String providerName) {
        strataprod.update("INSERT INTO LOBBY_USER (PLAYER_ID, PROVIDER_NAME, EXTERNAL_ID) VALUES (?,?,?)", playerId, providerName, externalId);
    }

    private void insertMarketingGroup(int id, String label) {
        strataproddw.update("INSERT INTO MARKETING_GROUP (ID, LABEL) VALUES(?,?)", id, label);
    }

    private void insertMarketingGroupMember(int playerGroupId, BigDecimal playerId, String gameType) {
        strataproddw.update("INSERT INTO MARKETING_GROUP_MEMBER(MARKETING_GROUP_ID, PLAYER_ID, GAME_TYPE) VALUES(?,?,?)", playerGroupId, playerId, gameType);
    }

    private class EquivalentAppRequestTargetMatcher extends TypeSafeDiagnosingMatcher<AppRequestTarget> {

        private final AppRequestTarget expectedTarget;

        public EquivalentAppRequestTargetMatcher(AppRequestTarget expectedTarget) {
            this.expectedTarget = expectedTarget;
        }

        @Override
        protected boolean matchesSafely(AppRequestTarget candidate, Description mismatchDescription) {
            if (!nullSafeEquals(candidate.getCampaignId(), expectedTarget.getCampaignId())) {
                mismatchDescription.appendText("campaignId ").appendValue(candidate.getCampaignId());
            } else if (!nullSafeEquals(candidate.getExternalId(), expectedTarget.getExternalId())) {
                mismatchDescription.appendText("externalId ").appendValue(candidate.getExternalId());
            } else if (!nullSafeEquals(candidate.getGameType(), expectedTarget.getGameType())) {
                mismatchDescription.appendText("gameType ").appendValue(candidate.getGameType());
            } else if (!nullSafeEquals(candidate.getExternalId(), expectedTarget.getExternalId())) {
                mismatchDescription.appendText("playerId ").appendValue(candidate.getPlayerId());
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description
                    .appendText("app-request-target with ")
                    .appendText(" campaignId ").appendValue(expectedTarget.getCampaignId())
                    .appendText(", externalId ").appendValue(expectedTarget.getExternalId())
                    .appendText(", gameType ").appendValue(expectedTarget.getGameType())
                    .appendText(", playerId ").appendValue(expectedTarget.getPlayerId());
        }
    }

    private void createAppRequestTargets() {
        // add some targets for
        AppRequestTarget androidTarget1 = new AppRequestTargetBuilder()
                .withId(1)
                .withAppRequestId(ANDROID1_ID)
                .withGameType("SLOTS")
                .withPlayerId(BigDecimal.valueOf(1234))
                .withExternalId("13243215")
                .build();
        AppRequestTarget androidTarget2 = new AppRequestTargetBuilder()
                .withId(2)
                .withAppRequestId(ANDROID1_ID)
                .withGameType("BLACKJACK")
                .withPlayerId(BigDecimal.valueOf(2))
                .withExternalId(null)
                .build();
        AppRequestTarget androidTarget3 = new AppRequestTargetBuilder()
                .withId(3)
                .withAppRequestId(ANDROID1_ID)
                .withGameType("BLACKJACK")
                .withPlayerId(BigDecimal.valueOf(3))
                .withExternalId(null)
                .build();
        existingAppRequestTargetsForAndroid = new AppRequestTarget[]{androidTarget1, androidTarget2, androidTarget3};
        for (AppRequestTarget appRequestTarget : existingAppRequestTargetsForAndroid) {
            strataproddw.update(
                    "INSERT INTO APP_REQUEST_TARGET (ID, APP_REQUEST_ID, GAME_TYPE, PLAYER_ID, EXTERNAL_ID)"
                            + " values(?,?,?,?,?)", appRequestTarget.getId(),
                    appRequestTarget.getCampaignId(), appRequestTarget.getGameType(),
                    appRequestTarget.getPlayerId(), appRequestTarget.getExternalId());
        }
    }

    private void setUpEngagementCampaigns() {
        EngagementCampaign engagementCampaignIOS1 = new EngagementCampaignBuilder()
                .withId(1)
                .withChannelType(ChannelType.IOS)
                .withTitle("IOS1")
                .withDescription("IOS1 DESC")
                .withMessage("Come back!")
                .withCreateDate(new DateTime())
                .withStatus(EngagementCampaignStatus.CREATED)
                .build();
        EngagementCampaign engagementCampaignIOS2 = new EngagementCampaignBuilder()
                .withId(2)
                .withChannelType(ChannelType.IOS)
                .withTitle("IOS2")
                .withDescription("IOS2 Campaign with Sent")
                .withMessage("Please Come back!")
                .withCreateDate(new DateTime())
                .withSentDate(new DateTime().plusDays(3))
                .withStatus(EngagementCampaignStatus.SENT)
                .withExpires(new DateTime().plusDays(5))
                .build();
        EngagementCampaign engagementCampaignANDROID1 = new EngagementCampaignBuilder()
                .withId(ANDROID1_ID)
                .withChannelType(ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID)
                .withTitle("ANDROID12")
                .withDescription("ANDROID1 DESC")
                .withMessage("R2D2 Come back!")
                .withTrackingReference("Track this droid")
                .withCreateDate(new DateTime())
                .withSentDate(new DateTime().plusDays(30))
                .withStatus(EngagementCampaignStatus.SENT)
                .withTargetCount(3)
                .withScheduled(new DateTime().plusDays(2))
                .build();
        EngagementCampaign engagementCampaignANDROID2 = new EngagementCampaignBuilder()
                .withId(ANDROID_ENGAGEMENT_CAMPAIGN_ID2)
                .withChannelType(ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID)
                .withTitle("ANDROID2")
                .withDescription("ANDROID2 DESC")
                .withMessage("R2oj ohhoioj ohi!")
                .withTrackingReference("trackety track clickty clack")
                .withCreateDate(new DateTime())
                .withSentDate(new DateTime().plusDays(30))
                .withStatus(EngagementCampaignStatus.SENT)
                .withScheduled(new DateTime().plusDays(2))
                .build();
        EngagementCampaign engagementCampaignIOS3 = new EngagementCampaignBuilder()
                .withId(3)
                .withChannelType(ChannelType.IOS)
                .withTitle("IOS3")
                .withDescription("IOS3 DESC")
                .withMessage("3 Please Come back!")
                .withCreateDate(new DateTime())
                .withSentDate(new DateTime().plusDays(33))
                .withStatus(EngagementCampaignStatus.SENT)
                .build();
        EngagementCampaign engagementCampaignFacebook = new EngagementCampaignBuilder()
                .withId(FACEBOOK_ENGAGAGEMENT_CAMPAIGN_ID)
                .withChannelType(ChannelType.FACEBOOK_APP_TO_USER_REQUEST)
                .withTitle("Facebook1")
                .withDescription("Facebook1 DESC")
                .withMessage("3 Please Come back!")
                .withCreateDate(new DateTime())
                .withSentDate(new DateTime().plusDays(33))
                .withStatus(EngagementCampaignStatus.SENT)
                .build();

        existingEngagementCampaigns = new EngagementCampaign[]{engagementCampaignIOS1, engagementCampaignANDROID1, engagementCampaignANDROID2, engagementCampaignIOS2, engagementCampaignIOS3, engagementCampaignFacebook};
    }

}
