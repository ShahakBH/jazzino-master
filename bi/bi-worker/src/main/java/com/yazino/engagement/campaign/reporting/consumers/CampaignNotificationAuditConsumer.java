package com.yazino.engagement.campaign.reporting.consumers;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.engagement.campaign.reporting.dao.CampaignNotificationAuditDao;
import com.yazino.engagement.campaign.reporting.domain.CampaignNotificationAuditMessage;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.google.common.collect.Sets.newLinkedHashSet;

@Component("campaignNotificationAuditConsumer")
public class CampaignNotificationAuditConsumer implements QueueMessageConsumer<CampaignNotificationAuditMessage>, CommitAware {

    private static final Logger LOG = LoggerFactory.getLogger(CampaignNotificationAuditConsumer.class);

    private final CampaignNotificationAuditDao campaignNotificationAuditDao;

    private final ThreadLocal<Set<CampaignNotificationAuditMessage>> batchedMessages =
            new ThreadLocal<Set<CampaignNotificationAuditMessage>>() {
                @Override
                protected Set<CampaignNotificationAuditMessage> initialValue() {
                    return new LinkedHashSet<CampaignNotificationAuditMessage>();
                }
            };

    @Autowired
    public CampaignNotificationAuditConsumer(CampaignNotificationAuditDao campaignNotificationAuditDao) {
        this.campaignNotificationAuditDao = campaignNotificationAuditDao;
    }

    @Override
    public void handle(CampaignNotificationAuditMessage message) {
        try {
            Set<CampaignNotificationAuditMessage> messages = getBatchedMessages();
            LOG.debug("Adding message {} to thread which contains {} messages", message, messages.size());
            messages.add(message);
        } catch (Exception e) {
            LOG.error("Error while saving audit message {}", message, e);
        }
    }

    @Override
    public void consumerCommitting() {
        final Set<CampaignNotificationAuditMessage> messagesInThread = getBatchedMessages();
        if (!messagesInThread.isEmpty()) {
            LOG.debug("Committing {}  messages for auditing", messagesInThread.size());
            campaignNotificationAuditDao.persist(newLinkedHashSet(messagesInThread));
            messagesInThread.clear();

        } else {
            LOG.trace("Nothing to commit, returning");
        }
    }

    protected Set<CampaignNotificationAuditMessage> getBatchedMessages() {
        return batchedMessages.get();
    }
}
