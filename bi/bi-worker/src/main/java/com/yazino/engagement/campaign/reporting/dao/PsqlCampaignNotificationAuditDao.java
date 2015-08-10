package com.yazino.engagement.campaign.reporting.dao;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.engagement.campaign.reporting.domain.CampaignNotificationAuditMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;

@Repository
public class PsqlCampaignNotificationAuditDao implements CampaignNotificationAuditDao {

    private static final Logger LOG = LoggerFactory.getLogger(PsqlCampaignNotificationAuditDao.class);

    private final JdbcTemplate externalDwJdbcTemplate;

    @Autowired
    public PsqlCampaignNotificationAuditDao(@Qualifier("externalDwJdbcTemplate") JdbcTemplate externalDwJdbcTemplate) {
        this.externalDwJdbcTemplate = externalDwJdbcTemplate;
    }

    @Override
    @Transactional("externalDwTransactionManager")
    public void persist(Set<CampaignNotificationAuditMessage> campaignNotificationAuditMessages) {
        try {

            externalDwJdbcTemplate.batchUpdate(new String[]{createInsertStatementForAuditMessages(campaignNotificationAuditMessages)});

        } catch (final DataAccessResourceFailureException e) {
            LOG.warn("Cannot connect to database, returning to queue", e);
            throw e;

        } catch (final TransientDataAccessException e) {
            LOG.warn("Transient failure while writing to database, returning to queue", e);
            throw e;

        } catch (final Exception e) {
            LOG.error("Save failed for messages: {}", campaignNotificationAuditMessages, e);
        }
    }

    private String createInsertStatementForAuditMessages(Set<CampaignNotificationAuditMessage> campaignNotificationAuditMessages) {

        InsertStatementBuilder insertStatementBuilder = new InsertStatementBuilder("CAMPAIGN_NOTIFICATION_AUDIT",
                "CAMPAIGN_RUN_ID", "PLAYER_ID", "CHANNEL", "GAME_TYPE", "NOTIFICATION_AUDIT_TYPE", "RUN_TS");

        for (CampaignNotificationAuditMessage message : campaignNotificationAuditMessages) {
            insertStatementBuilder = insertStatementBuilder.withValues(
                    sqlLong(message.getCampaignRunId()),
                    sqlBigDecimal(message.getPlayerId()),
                    sqlString(message.getChannel().toString()),
                    sqlString(message.getGameType()),
                    sqlString(message.getNotificationAuditType().toString()),
                    sqlTimestamp(message.getTimeStamp())
            );
        }

        return insertStatementBuilder.toSql();
    }
}
