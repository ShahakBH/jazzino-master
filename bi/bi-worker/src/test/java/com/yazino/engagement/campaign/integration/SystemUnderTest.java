package com.yazino.engagement.campaign.integration;

import com.yazino.bi.campaign.dao.CampaignAddTargetDao;
import com.yazino.bi.campaign.dao.CampaignDefinitionDao;
import com.yazino.bi.campaign.dao.CampaignScheduleDao;
import com.yazino.bi.campaign.domain.CampaignDefinition;
import com.yazino.bi.campaign.domain.CampaignSchedule;
import com.yazino.bi.payment.persistence.JDBCPaymentOptionDAO;
import com.yazino.bi.persistence.BatchVisitor;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.campaign.application.*;
import com.yazino.engagement.campaign.consumers.CampaignDeliveryConsumer;
import com.yazino.engagement.campaign.consumers.CampaignRunConsumer;
import com.yazino.engagement.campaign.dao.CampaignRunDao;
import com.yazino.engagement.campaign.dao.MySqlLockDao;
import com.yazino.engagement.campaign.dao.SegmentSelectorDao;
import com.yazino.engagement.campaign.domain.CampaignRun;
import com.yazino.engagement.campaign.domain.CampaignRunMessage;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import com.yazino.engagement.campaign.reporting.application.AuditDeliveryService;
import com.yazino.engagement.campaign.reporting.application.AuditDeliveryServiceImpl;
import com.yazino.engagement.campaign.reporting.consumers.CampaignAuditConsumer;
import com.yazino.engagement.campaign.reporting.dao.CampaignAuditDao;
import com.yazino.engagement.campaign.reporting.domain.CampaignAuditMessage;
import com.yazino.game.api.GameType;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.messaging.Message;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.promotions.*;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import strata.server.lobby.api.promotion.*;
import strata.server.operations.promotion.model.ChipPackage;
import strata.server.operations.promotion.service.PaymentOptionsToChipPackageTransformer;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SystemUnderTest {
    private final HashMap<Long, Boolean> createdPromosHash = new HashMap<>();
    private int playerIdCounter = 1;
    private Long promoDefIdCounter = 1l;
    private Long promoIdCounter = 1l;

    private Map<String, Map<String, Object>> players = new HashMap<>();
    private List<PlayerMessage> playerReceivedMessage = new ArrayList<>();
    private YazinoConfiguration yazinoConfiguration = new YazinoConfiguration();
    //campaign-defining classes
    private final DummySegmentSelectorDao segmentSelectorDao = new DummySegmentSelectorDao();

    private final DummyCampaignRunDao campaignRunDao = new DummyCampaignRunDao();
    private final CampaignDefinitionDao campaignDefinitionDao = new DummyCampaignDefinitionDao();
    //scheduling-related classes
    private final CampaignScheduleDao campaignScheduleDao = new DummyCampaignScheduleDao();

    private PromotionDefinitionDao promotionDefinitionDao = mock(PromotionDefinitionDao.class);
    private CampaignContentService campaignContentService = new DummyCampaignContentService();

    private final DummyQueuePublishing<CampaignRunMessage> campaignRunQueue = new DummyQueuePublishing<>();

    private final CampaignScheduler campaignScheduler = new CampaignScheduler(
            campaignScheduleDao,
            new CampaignRunService(campaignRunQueue),
            new DummyLockDao(),
            yazinoConfiguration);
    private final PromotionDao promotionDao = new DummyPromotionDao();
    private final PromotionFormDefinitionDao buyChipsPromotionDefinitionDao = new DummyBuyChipsPromotionDefinitionDao();
    private final PromotionFormDefinitionDao dailyAwardPromotionDefinitionDao = new DummyDailyAwardPromotionDefinitionDao();
    private PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer = new DummyPaymentOptionsToChipPackageTransformer();

    private GiftingPromotionDefinitionDao giftingPromotionDefinitionDao = new GiftingPromotionDefinitionDao(Mockito.mock(NamedParameterJdbcTemplate.class));
    private final PromotionCreationService promotionCreationService = new PromotionCreationService(promotionDao, buyChipsPromotionDefinitionDao, dailyAwardPromotionDefinitionDao, paymentOptionsToChipPackageTransformer, promotionDefinitionDao, giftingPromotionDefinitionDao);

    // auditing classes
    private final Map<Long, Map<String, String>> campaignRunTable = new HashMap<>();
    private QueuePublishing<CampaignAuditMessage> campaignAuditDeliveryQueue = new QueuePublishing<>();
    private AuditDeliveryService auditDeliveryService = new AuditDeliveryServiceImpl(campaignAuditDeliveryQueue);

    //delivery-related classes
    private final DummyQueuePublishing<CampaignDeliverMessage> campaignDeliveryQueue = new DummyQueuePublishing<>();
    private final CampaignDeliveryService campaignDeliveryService = new CampaignDeliveryService(campaignDeliveryQueue);
    private final CampaignAddTargetDao dummyCampaignAddTargetDao = new DummyCampaignAddTargetDao();
    private final CampaignService campaignService = new CampaignService(campaignDefinitionDao, campaignRunDao, segmentSelectorDao, campaignDeliveryService, auditDeliveryService, promotionCreationService, campaignContentService, dummyCampaignAddTargetDao);

    //running-related classes
    private final CampaignRunConsumer campaignRunConsumer = new CampaignRunConsumer(campaignService);


    public SystemUnderTest() {
        when(promotionDefinitionDao.getPromotionDefinitionType(anyLong())).thenReturn(PromotionType.BUY_CHIPS);
        final ChannelCampaignDeliveryAdapter dummyAdapterIos = new DummyAdapter(campaignRunDao, ChannelType.IOS, this);
        final ChannelCampaignDeliveryAdapter dummyAdapterGoogle = new DummyAdapter(campaignRunDao, ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, this);
        final ChannelCampaignDeliveryAdapter dummyAdapterFacebook = new DummyAdapter(campaignRunDao, ChannelType.FACEBOOK_APP_TO_USER_REQUEST, this);
        final CampaignDeliveryConsumer campaignDeliveryConsumer = new CampaignDeliveryConsumer(newArrayList(dummyAdapterIos,
                dummyAdapterGoogle,
                dummyAdapterFacebook));
        campaignDeliveryQueue.deliverDirectlyTo(campaignDeliveryConsumer);
        campaignRunQueue.deliverDirectlyTo(campaignRunConsumer);
        final DummyCampaignAuditDao dummyCampaignAuditDao = new DummyCampaignAuditDao();
        final QueueMessageConsumer<CampaignAuditMessage> campaignAuditConsumer = new CampaignAuditConsumer(dummyCampaignAuditDao);
        campaignAuditDeliveryQueue.deliverDirectlyTo(campaignAuditConsumer);
    }

    public void createPlayers(final String... playerNames) {
        for (String playerName : playerNames) {
            final HashMap<String, Object> playerAttrs = new HashMap<>();
            playerAttrs.put("playerId", BigDecimal.valueOf(playerIdCounter++));
            playerAttrs.put("playerName", playerName);
            players.put(playerName, playerAttrs);
        }
    }

    public void playedYesterday(final String playerName) {
        segmentSelectorDao.addPlayer((BigDecimal) players.get(playerName).get("playerId"));
    }

    public boolean playerReceivedMessage(final String playerName, final ChannelType channel, final String message) {
        for (PlayerMessage playerMessage : playerReceivedMessage) {
            if (playerMessage.playerName.equals(playerName) && playerMessage.receivedTs.equals(new DateTime().getMillis()) && playerMessage.channel.equals(channel)
                    && playerMessage.message.equals(message)) {
                return true;
            }
        }
        return false;
    }

    public HashSet<String> playerReceivedMessages(String playerName) {
        HashSet<String> receivedMessages = new HashSet<>();
        for (PlayerMessage playerMessage : playerReceivedMessage) {
            if (playerMessage.playerName.equals(playerName) && playerMessage.receivedTs.equals(new DateTime().getMillis())) {
                receivedMessages.add(playerMessage.message);
            }
        }
        return receivedMessages;
    }

    public void schedulerRunsCampaign() {
        campaignScheduler.runScheduledCampaign();
    }

    public void campaignRunConsumerReadsMessage(final Long campaignId) {
        campaignRunConsumer.handle(new CampaignRunMessage(campaignId, new Date()));
    }

    public Boolean isCampaignRunPersistedForCampaign(Long campaignId) {
        return campaignRunTable.containsKey(campaignId);
    }

    public Map<Long, Map<String, String>> getCampaignRunAudits() {
        return campaignRunTable;
    }

    public void createGiftingCampaign(final String title, final String message, final String description, final int amountOfChips, final int hoursToLive, final DateTime start) {
        createCampaign(title, message, start, true);
        createGiftingPromo();
    }

    private void createGiftingPromo() {

    }

    public Long createCampaign(final String name, final String message, final DateTime firstRunTs, final Boolean hasPromo) {
        HashMap<String, String> content = new HashMap<>();
        content.put("message", message);

        final Map<NotificationChannelConfigType, String> channelConfig = newHashMap();
        CampaignDefinition campaign = new CampaignDefinition(null, name, "select 1 as 1", content, asList(
                ChannelType.IOS,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID,
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST),
                hasPromo, channelConfig, hasPromo,
                false
        );

        Long campaignId = campaignDefinitionDao.save(campaign);
        campaignScheduleDao.updateNextRunTs(campaignId, firstRunTs);

        return campaignId;
    }

    public void sendMessageToPlayers(final BigDecimal playerId, Long campaignRunId, ChannelType channelType, String message) {
        for (Map<String, Object> player : players.values()) {
            if (playerId.equals(player.get("playerId"))) {
                playerReceivedMessage.add(new PlayerMessage((String) player.get("playerName"), channelType, campaignRunId, message));
            }
        }
    }

    public void onTheNextDay() {
        segmentSelectorDao.clear();
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis() + DateTimeConstants.MILLIS_PER_DAY);
    }

    public void onNextWeek() {
        segmentSelectorDao.clear();
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis() + DateTimeConstants.MILLIS_PER_WEEK);
    }

    public void campaignServiceRunsCampaign(final Long campaignId, final DateTime reportTime) {
        campaignService.runCampaign(campaignId, reportTime);
    }

    public Boolean hasCreatedPromotion() {
        if (createdPromosHash.size() > 0) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public void createBuyChipsPromotionWith(final BuyChipsForm buyChipsForm) {
        buyChipsPromotionDefinitionDao.save(buyChipsForm);
    }

    public Promotion getLastCreatedPromotion() {
        return promotionDao.findById(promoIdCounter - 1);
    }

    private class PlayerMessage {
        public String playerName;
        public ChannelType channel;
        public Long receivedTs;
        public String message;

        private PlayerMessage(String playerName, ChannelType channel, Long receivedTs, String message) {
            this.playerName = playerName;
            this.channel = channel;
            this.receivedTs = receivedTs;
            this.message = message;
        }
    }


    public class DummyCampaignDefinitionDao implements CampaignDefinitionDao {
        private Map<Long, CampaignDefinition> campaigns = new HashMap<>();
        private int campaignId = 0;

        public DummyCampaignDefinitionDao() {
        }

        @Override
        public CampaignDefinition fetchCampaign(final Long campaignId) {
            return campaigns.get(campaignId);
        }

        @Override
        public Long save(final CampaignDefinition campaignDefinition) {
            Long newId = (long) campaignId++;
            final Map<NotificationChannelConfigType, String> channelConfig = newHashMap();
            campaigns.put(newId, new CampaignDefinition(
                    newId,
                    campaignDefinition.getName(),
                    campaignDefinition.getSegmentSelectionQuery(),
                    campaignDefinition.getContent(),
                    campaignDefinition.getChannels(),
                    campaignDefinition.hasPromo(), channelConfig, true, false));
            return newId;
        }

        @Override
        public Map<String, String> getContent(final Long campaignId) {
            return campaigns.get(campaignId).getContent();
        }

        @Override
        public void update(final CampaignDefinition campaignDefinition) {
            throw new UnsupportedOperationException("only used by bi-operations atm");
        }

        @Override
        public void setEnabledStatus(final Long campaignId, final boolean enabled) {

        }

        @Override
        public Map<NotificationChannelConfigType, String> getChannelConfig(final Long campaignId) {
            return new HashMap<>();
        }

        @Override
        public List<ChannelType> getChannelTypes(final Long campaignId) {
            return null;
        }
    }

    public class DummyQueuePublishing<T extends Message> implements QueuePublishingService<T> {

        private QueueMessageConsumer<T> consumer;

        @Override
        public void send(T t) {
            consumer.handle(t);
        }

        public void deliverDirectlyTo(final QueueMessageConsumer<T> campaignDeliveryConsumer) {
            this.consumer = campaignDeliveryConsumer;
        }
    }

    public class QueuePublishing<T extends Message> implements QueuePublishingService<T> {

        private QueueMessageConsumer<T> consumer;

        @Override
        public void send(T t) {
            consumer.handle(t);
        }

        public void deliverDirectlyTo(final QueueMessageConsumer<T> campaignAuditConsumer) {
            this.consumer = campaignAuditConsumer;
        }
    }

    public class DummyCampaignRunDao implements CampaignRunDao {

        private Map<Long, List<PlayerWithContent>> campaignRunPlayers = newHashMap();
        private long runId = 1;
        private Map<Long, Long> runToCampaign = new HashMap<>();

        @Override
        public Long createCampaignRun(final Long campaignId, final DateTime dateTime) {
            Long newRunId = runId++;
            runToCampaign.put(newRunId, campaignId);
            campaignRunPlayers.put(newRunId, new ArrayList<PlayerWithContent>());
            return newRunId;
        }

        @Override
        public void addPlayers(final Long campaignRunId, final Collection<PlayerWithContent> toDeliver, final boolean b) {
            campaignRunPlayers.get(campaignRunId).addAll(toDeliver);
        }

        @Override
        public List<PlayerWithContent> fetchPlayers(Long campaignRunId) {
            return campaignRunPlayers.get(campaignRunId);
        }

        @Override
        public CampaignRun getCampaignRun(final Long campaignRunId) {
            return new CampaignRun(campaignRunId, runToCampaign.get(campaignRunId), new DateTime());
        }

        @Override
        public Map<Long, Long> getLatestDelayedCampaignRunsInLast24Hours() {
            return null;
        }

        @Override
        public DateTime getLastRuntimeForCampaignRunIdAndResetTo(final Long campaignRunId, final DateTime now) {
            return null;
        }

        @Override
        public void purgeSegmentSelection(final Long campaignRunId) {

        }
    }

    public class DummySegmentSelectorDao implements SegmentSelectorDao {
        List<PlayerWithContent> playerIdsForSegment = new ArrayList<>();

        @Override
        public int fetchSegment(final String segmentSelectionQuery, final DateTime reportTime, final BatchVisitor<PlayerWithContent> visitor) {
            visitor.processBatch(playerIdsForSegment);
            return playerIdsForSegment.size();
        }

        @Override
        public void updateSegmentDelaysForCampaignRuns(final Set<Long> campaignIds, final DateTime now) {
            //do nothing
        }

        public void addPlayer(final BigDecimal playerId) {
            playerIdsForSegment.add(new PlayerWithContent(playerId));
        }

        public void clear() {
            playerIdsForSegment.clear();
        }
    }

    public class DummyAdapter implements ChannelCampaignDeliveryAdapter {

        private CampaignRunDao campaignRunDao;
        private ChannelType channel;
        private SystemUnderTest systemUnderTest;

        public DummyAdapter(CampaignRunDao campaignRunDao, ChannelType channel, SystemUnderTest systemUnderTest) {
            this.campaignRunDao = campaignRunDao;
            this.channel = channel;
            this.systemUnderTest = systemUnderTest;
        }

        @Override
        public ChannelType getChannel() {
            return channel;
        }

        @Override
        public void sendMessageToPlayers(final CampaignDeliverMessage message) {
            List<PlayerWithContent> playerIds = campaignRunDao.fetchPlayers(message.getCampaignRunId());
            Map<String, String> content = campaignContentService.getContent(message.getCampaignRunId());
            long sendTs = new DateTime().getMillis();
            for (PlayerWithContent playerId : playerIds) {
                systemUnderTest.sendMessageToPlayers(playerId.getPlayerId(), sendTs, message.getChannel(), content.get("message"));
            }
        }
    }

    public class DummyCampaignScheduleDao implements CampaignScheduleDao {
        private Map<Long, DateTime> campaignRunTs = new HashMap<>();

        @Override
        public void updateNextRunTs(Long campaignId, DateTime nextRunTs) {
            campaignRunTs.put(campaignId, nextRunTs);
        }

        @Override
        public List<CampaignSchedule> getDueCampaigns(DateTime currentTimestamp) {
            ArrayList<CampaignSchedule> result = new ArrayList<>();
            DateTime now = new DateTime();
            for (Map.Entry<Long, DateTime> scheduleEntry : campaignRunTs.entrySet()) {
                if (scheduleEntry.getValue().isBefore(now) || scheduleEntry.getValue().isEqual(now)) {
                    result.add(new CampaignSchedule(scheduleEntry.getKey(), scheduleEntry.getValue(), 168l, 0l, null));
                }
            }
            return result;
        }

        @Override
        public void save(final CampaignSchedule campaignSchedule) {
            throw new UnsupportedOperationException("only used by bi-operations atm");
        }

        @Override
        public void update(final CampaignSchedule campaignSchedule) {
            throw new UnsupportedOperationException("only used by bi-operations atm");
        }

        @Override
        public CampaignSchedule getCampaignSchedule(final Long campaignId) {
            throw new UnsupportedOperationException("only used by bi-operations atm");
        }

    }

    public class DummyLockDao extends MySqlLockDao {
        @Override
        public boolean lock(String lockName, String clientId) {
            return true;
        }

        @Override
        public void unlock(String lockName, String clientId) {
        }
    }

    public class DummyCampaignAuditDao implements CampaignAuditDao {
        @Override
        public void persistCampaignRun(final Long campaignId,
                                       final Long campaignRunId,
                                       final String name,
                                       final int size,
                                       final DateTime timestamp,
                                       final String status,
                                       final String message, final Long promoId) {

            final Map<String, String> record = new HashMap<>();

            record.put("campaignRunId", campaignRunId.toString());
            record.put("campaignId", campaignId.toString());
            record.put("name", name);
            record.put("targetSize", String.valueOf(size));
            record.put("timestamp", timestamp.toString());
            record.put("status", status);
            record.put("message", message);

            campaignRunTable.put(campaignId, record);
        }
    }

    public class DummyPromotionDao implements PromotionDao {
        final HashMap<Long, Set<BigDecimal>> playerHash = new HashMap<>();
        final HashMap<Long, Promotion> promoHash = new HashMap<>();

        @Override
        public Long create(final Promotion promo) {
            createdPromosHash.put(promoIdCounter, Boolean.TRUE);
            promoHash.put(promoIdCounter, promo);
            return promoIdCounter++;
        }

        @Override
        public void update(final Promotion promo) {

        }

        @Override
        public void delete(final Long promoId) {

        }

        @Override
        public void addPlayersTo(final Long promoId, final Set<BigDecimal> playerIds) {
            playerHash.put(promoId, playerIds);
        }

        @Override
        public void updatePlayerCountInPromotion(final Long promoId) {

        }

        @Override
        public List<Promotion> findWebPromotions(final BigDecimal playerId, final DateTime applicableDate) {
            return null;
        }

        @Override
        public List<Promotion> findWebPromotionsOrderedByPriority(final BigDecimal playerId, final DateTime applicableDate) {
            return null;
        }

        @Override
        public List<Promotion> findPromotionsForCurrentTime(final BigDecimal playerId, final PromotionType type, final Platform platform) {
            return null;
        }

        @Override
        public List<Promotion> findPromotionsFor(final BigDecimal playerId, final PromotionType type, final Platform platform, final DateTime currentTime) {
            return null;
        }

        @Override
        public List<DailyAwardPromotion> getWebDailyAwardPromotions(final BigDecimal playerId, final DateTime currentTime) {
            return null;
        }

        @Override
        public List<ProgressiveDailyAwardPromotion> getProgressiveDailyAwardPromotion(final BigDecimal playerId, final DateTime currentTime, final ProgressiveAwardEnum progressiveAward) {
            return null;
        }

        @Override
        public List<DailyAwardPromotion> getIosDailyAwardPromotions(final BigDecimal playerId, final DateTime currentTime) {
            return null;
        }

        @Override
        public void addLastReward(final PromotionPlayerReward promotionPlayerReward) {

        }

        @Override
        public Map<PaymentPreferences.PaymentMethod, Promotion> getBuyChipsPromotions(final BigDecimal playerId, final Platform platform, final DateTime applicableDate) {
            return null;
        }

        @Override
        public Promotion findById(final Long promoId) {
            return promoHash.get(promoId);
        }

        @Override
        public List<Promotion> findPromotionsByTypeOrderByPriority(final BigDecimal playerId, final PromotionType type, final Platform platform, final DateTime currentTime) {
            return null;
        }

        @Override
        public List<BigDecimal> getProgressiveAwardPromotionValueList() {
            return null;
        }

        @Override
        public List<PromotionPlayerReward> findPromotionPlayerRewards(final BigDecimal playerId, final DateTime topUpDate) {
            return null;
        }

        @Override
        public void associateMarketingGroupMembersWithPromotion(final int marketingGroupId, final Long promoId) {

        }
    }

    public class DummyBuyChipsPromotionDefinitionDao implements PromotionFormDefinitionDao<BuyChipsForm> {
        HashMap<Long, BuyChipsForm> content = new HashMap<>();

        @Override
        public Long save(final BuyChipsForm buyChipsForm) {
            content.put(buyChipsForm.getCampaignId(), buyChipsForm);

            return Long.valueOf(promoDefIdCounter++);
        }

        @Override
        public void update(final BuyChipsForm buyChipsForm) {
            content.remove(buyChipsForm.getCampaignId());
            content.put(buyChipsForm.getCampaignId(), buyChipsForm);
        }

        @Override
        public BuyChipsForm getForm(final Long campaignId) {
            return content.get(campaignId);
        }


    }

    public class DummyDailyAwardPromotionDefinitionDao implements PromotionFormDefinitionDao<DailyAwardForm> {
        HashMap<Long, DailyAwardForm> content = new HashMap<>();

        @Override
        public Long save(final DailyAwardForm buyChipsForm) {
            content.put(buyChipsForm.getCampaignId(), buyChipsForm);

            return Long.valueOf(promoDefIdCounter++);
        }

        @Override
        public void update(final DailyAwardForm buyChipsForm) {
            content.remove(buyChipsForm.getCampaignId());
            content.put(buyChipsForm.getCampaignId(), buyChipsForm);
        }

        @Override
        public DailyAwardForm getForm(final Long campaignId) {
            return content.get(campaignId);
        }


    }

    public class DummyPaymentOptionsToChipPackageTransformer extends PaymentOptionsToChipPackageTransformer {

        public DummyPaymentOptionsToChipPackageTransformer() {
            super(mock(JDBCPaymentOptionDAO.class));
        }

        @Override
        public Map<Platform, List<ChipPackage>> getDefaultPackages() {
            Map<Platform, List<ChipPackage>> defaultPackages = newHashMap();
            for (Platform platform : Platform.values()) {
                defaultPackages.put(platform, new ArrayList<ChipPackage>());
            }
            return defaultPackages;
        }
    }

    private class DummyCampaignContentService implements CampaignContentService {
        @Override
        public void updateCustomDataFields(final Long campaignRunId, final Map<String, String> campaignContent) {

        }

        @Override
        public Map<String, String> personaliseContentData(final Map<String, String> campaignContent, final Map<String, String> customData, final GameType gameType) {
            return campaignContent;
        }

        @Override
        public Map<String, String> getPersonalisedContent(final Map<String, String> campaignContent, final PlayerTarget playerTarget) {
            return null;
        }

        @Override
        public Map<String, String> getContent(final Long campaignRunId) {
            final CampaignRun campaignRun = campaignRunDao.getCampaignRun(campaignRunId);

            return campaignDefinitionDao.getContent(campaignRun.getCampaignId());
        }

        @Override
        public String getEmailListName(final Long campaignRunId) {
            return null;
        }

        @Override
        public Map<NotificationChannelConfigType, String> getChannelConfig(final Long campaignRunId) {
            return campaignDefinitionDao.getChannelConfig(campaignRunId);
        }

    }

    private class DummyCampaignAddTargetDao implements CampaignAddTargetDao {

        @Override
        public void savePlayersToCampaign(final Long campaignId, final Set<BigDecimal> expectedPlayerIds) {

        }

        @Override
        public int fetchCampaignTargets(final Long campaignId, final BatchVisitor<PlayerWithContent> visitor) {
            return 0;
        }

        @Override
        public Integer numberOfTargetsInCampaign(final Long campaignId) {
            return 1;
        }
    }
}
