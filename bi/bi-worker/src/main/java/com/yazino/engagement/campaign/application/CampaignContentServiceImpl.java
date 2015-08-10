package com.yazino.engagement.campaign.application;

import com.yazino.bi.campaign.dao.CampaignDefinitionDao;
import com.yazino.bi.campaign.domain.CampaignDefinition;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.campaign.dao.CampaignContentDao;
import com.yazino.engagement.campaign.dao.CampaignRunDao;
import com.yazino.engagement.campaign.domain.CampaignRun;
import com.yazino.engagement.campaign.domain.MessageContentType;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.engagement.campaign.domain.NotificationCustomField;
import com.yazino.game.api.GameType;
import com.yazino.platform.table.GameTypeInformation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.operations.repository.GameTypeRepository;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Sets.newHashSet;

@Service
public class CampaignContentServiceImpl implements CampaignContentService {
    private static final Logger LOG = LoggerFactory.getLogger(CampaignContentServiceImpl.class);
    private final CampaignContentDao campaignContentDao;
    private final CampaignRunDao campaignRunDao;
    private final CampaignDefinitionDao campaignDefinitionDao;
    private final Map<String, GameTypeInformation> gameTypes;


    @Autowired
    public CampaignContentServiceImpl(final CampaignContentDao campaignContentDao,
                                      final CampaignRunDao campaignRunDao,
                                      final CampaignDefinitionDao campaignDefinitionDao,
                                      final GameTypeRepository gameTypeRepository) {
        Validate.notNull(campaignContentDao, "campaignContentDao should not be null");
        Validate.notNull(campaignRunDao, "campaignRunDao should not be null");
        Validate.notNull(campaignDefinitionDao, "campaignDefinition should not be null");
        Validate.notNull(gameTypeRepository, "gameTypeRepository should not be null");

        this.campaignContentDao = campaignContentDao;
        this.campaignDefinitionDao = campaignDefinitionDao;
        this.campaignRunDao = campaignRunDao;
        this.gameTypes = gameTypeRepository.getGameTypes();
    }

    @Override
    public void updateCustomDataFields(final Long campaignRunId, final Map<String, String> campaignContent) {

        final String messageText = getCustomFieldValueFromContent(campaignContent, MessageContentType.MESSAGE.getKey());
        LOG.debug("Message is {}, Custom fields are {}", messageText, getCustomFields(messageText));

        for (String customField : getCustomFields(messageText)) {
            try {
                switch (NotificationCustomField.valueOf(customField)) {
                    case PROGRESSIVE:
                        campaignContentDao.fillProgressiveBonusAmount(campaignRunId);
                        break;
                    case DISPLAY_NAME:
                        campaignContentDao.fillDisplayName(campaignRunId);
                        break;
                    default:
                        LOG.warn("unknown place holder {}", customField);
                        break;
                }
            } catch (IllegalArgumentException e) {
                //no need to worry, we handle more than just these two now.
            }
        }
    }

    private Set<String> getCustomFields(final String messageText) {
        final Set<String> result = newHashSet();
        if (messageText != null) {
            final Matcher matcher = Pattern.compile("\\{\\w+\\}").matcher(messageText);
            while (matcher.find()) {
                final String placeholder = matcher.group(0);
                LOG.debug("adding placeholder {}", placeholder);
                result.add(placeholder.substring(1, placeholder.length() - 1));
            }
        }
        return result;
    }

    /**
     * @deprecated replaced by {@link #getPersonalisedContent(java.util.Map, com.yazino.engagement.PlayerTarget)} ()}
     */
    @Override
    public Map<String, String> personaliseContentData(final Map<String, String> campaignContent,
                                                      final Map<String, String> customData,
                                                      final GameType gameType) {
        final HashMap<String, String> modifiedContent = new HashMap<>(campaignContent);
        final String originalTitleData = getCustomFieldValueFromContent(campaignContent, MessageContentType.TITLE.getKey());
        final String originalMessageData = getCustomFieldValueFromContent(campaignContent, MessageContentType.MESSAGE.getKey());
        LOG.debug("original Campaign content = {}", campaignContent);

        modifiedContent.put(MessageContentType.TITLE.getKey(), getBlankTitleToBeGameType(gameType, getMessageWithCustomData(customData, originalTitleData)));
        modifiedContent.put(MessageContentType.MESSAGE.getKey(), getMessageWithCustomData(customData, originalMessageData));
        LOG.debug("modifiedContent = {}", modifiedContent);
        return modifiedContent;
    }

    @Override
    public Map<String, String> getPersonalisedContent(final Map<String, String> campaignContent, final PlayerTarget playerTarget) {
        final Map<String, String> personalData = playerTarget.getCustomData();

        final HashMap<String, String> modifiedContent = new HashMap<>(campaignContent);
        final String originalTitleData = getCustomFieldValueFromContent(campaignContent, MessageContentType.TITLE.getKey());
        final String originalMessageData = getCustomFieldValueFromContent(campaignContent, MessageContentType.MESSAGE.getKey());
        LOG.debug("original Campaign content = {}", campaignContent);

        modifiedContent.put(MessageContentType.TITLE.getKey(), getBlankTitleToBeGameType(gameTypes.get(playerTarget.getGameType()).getGameType(),
                                                                                         getMessageWithCustomData(personalData, originalTitleData)));
        modifiedContent.put(MessageContentType.MESSAGE.getKey(), getMessageWithCustomData(personalData, originalMessageData));
        LOG.debug("modifiedContent = {}", modifiedContent);

        return modifiedContent;
    }

    private String getBlankTitleToBeGameType(final GameType gameType, final String originalTitleData) {
        if (StringUtils.isEmpty(originalTitleData) && gameType != null) {
            return gameType.getName();
        } else {
            return originalTitleData;
        }
    }

    private String getMessageWithCustomData(final Map<String, String> customData, final String originalMessageData) {
        String customFieldValue;
        String modifiedMessage = originalMessageData;

        for (String key : customData.keySet()) {
            if (key.equals(NotificationCustomField.PROGRESSIVE.name())) {
                final Long progressiveValue = Long.valueOf(getCustomFieldValueFromContent(customData, key));
                customFieldValue = formatChipAmount(progressiveValue);
            } else {
                customFieldValue = getCustomFieldValueFromContent(customData, key);
            }

            modifiedMessage = replaceFieldWithValueInMessage(modifiedMessage, customFieldValue, key);
        }
        modifiedMessage = removeAnyUnusedTags(modifiedMessage);
        return modifiedMessage;
    }

    private String removeAnyUnusedTags(final String modifiedMessage) {
        if (modifiedMessage != null) {
            final String[] split = modifiedMessage.split("\\{\\w+\\}");
            return StringUtils.join(split);
        } else {
            return null;
        }
    }

    private String replaceFieldWithValueInMessage(final String modifiedMessage, final String customFieldValue, final String notificationCustomField) {
        return StringUtils.replace(modifiedMessage,
                                   getCustomFieldWithBraces(notificationCustomField),
                                   customFieldValue);
    }

    private String formatChipAmount(final Long progressiveValue) {
        final String customFieldValue;
        DecimalFormat formatter = new DecimalFormat("#,###");
        customFieldValue = formatter.format(progressiveValue);
        return customFieldValue;
    }

    private String getCustomFieldValueFromContent(final Map<String, String> customData, final String notificationCustomField) {
        return customData.get(notificationCustomField);
    }

    private String getCustomFieldWithBraces(final String notificationCustomField) {
        return new StringBuilder("{")
                .append(notificationCustomField)
                .append("}")
                .toString();
    }

    @Override
    public Map<String, String> getContent(final Long campaignRunId) {
        final CampaignRun campaignRun = campaignRunDao.getCampaignRun(campaignRunId);
        return campaignDefinitionDao.getContent(campaignRun.getCampaignId());
    }

    @Override
    public Map<NotificationChannelConfigType, String> getChannelConfig(final Long campaignRunId) {
        final CampaignRun campaignRun = campaignRunDao.getCampaignRun(campaignRunId);
        return campaignDefinitionDao.getChannelConfig(campaignRun.getCampaignId());
    }

    @Override
    public String getEmailListName(final Long campaignRunId) {
        final CampaignRun campaignRun = campaignRunDao.getCampaignRun(campaignRunId);
        final CampaignDefinition campaignDefinition = campaignDefinitionDao.fetchCampaign(campaignRun.getCampaignId());
        LOG.debug("campaign run is {}", campaignRun);
        LOG.debug("campaign definition is {}", campaignDefinition);

        final String dateString = new SimpleDateFormat("ddMMyy_HHmm-").format(campaignRun.getRunTimestamp().toDate());
        final String title = campaignDefinition.getName().replace(" ", "_");

        final StringBuilder stringBuilder = new StringBuilder(dateString);
        stringBuilder.append(title).append("-").append(campaignRun.getCampaignRunId()).append(".csv");

        return stringBuilder.toString();
    }
}
