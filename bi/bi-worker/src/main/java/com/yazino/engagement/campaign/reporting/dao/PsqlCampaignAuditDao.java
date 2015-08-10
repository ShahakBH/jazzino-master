package com.yazino.engagement.campaign.reporting.dao;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class PsqlCampaignAuditDao implements CampaignAuditDao {
    private static final Logger LOG = LoggerFactory.getLogger(PsqlCampaignAuditDao.class);
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public PsqlCampaignAuditDao(@Qualifier("externalDwNamedJdbcTemplate") final NamedParameterJdbcTemplate template) {
        notNull(template);
        this.template = template;
    }

    @Override
    public void persistCampaignRun(final Long campaignId,
                                   final Long campaignRunId,
                                   final String name,
                                   final int size,
                                   final DateTime timestamp,
                                   final String status,
                                   final String message,
                                   final Long promoId) {

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("campaignId", campaignId);
        paramMap.put("campaignRunId", campaignRunId);
        paramMap.put("name", name);
        paramMap.put("size", size);
        paramMap.put("timestamp", new Timestamp(timestamp.getMillis()));
        paramMap.put("status", status);
        paramMap.put("message", message);
        paramMap.put("promoId", promoId);

        LOG.debug("persistting campaign run to dw database, {}", paramMap);
        template.update("INSERT INTO CAMPAIGN_RUN_AUDIT (CAMPAIGN_ID, RUN_ID, NAME, SEGMENT_SIZE, RUN_TS, STATUS, MESSAGE, PROMO_ID) "
                + "VALUES (:campaignId, :campaignRunId, :name, :size, :timestamp, :status, :message, :promoId)", paramMap);

    }
}
