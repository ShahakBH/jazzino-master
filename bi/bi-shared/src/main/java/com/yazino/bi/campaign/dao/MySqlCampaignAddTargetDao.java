package com.yazino.bi.campaign.dao;

import com.yazino.bi.persistence.BatchResultSetExtractor;
import com.yazino.bi.persistence.BatchVisitor;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class MySqlCampaignAddTargetDao implements CampaignAddTargetDao {
    private static final int DEFAULT_BATCH_SIZE = 10000;
    private static final String PROPERTY_BATCH_SIZE = "strata.worker.campaign.batch-size";

    private final JdbcTemplate template;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public MySqlCampaignAddTargetDao(@Qualifier("dwJdbcTemplate") final JdbcTemplate template,
                                     final YazinoConfiguration yazinoConfiguration) {
        notNull(template, "template may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.template = template;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @Override
    public void savePlayersToCampaign(final Long campaignId,
                                      final Set<BigDecimal> playerIds) {
        final List<BigDecimal> orderedPlayerIds = new ArrayList<>(playerIds);

        template.batchUpdate("INSERT IGNORE INTO CAMPAIGN_TARGET (CAMPAIGN_ID, PLAYER_ID) VALUES (?,?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(final PreparedStatement ps, final int i) throws SQLException {
                        ps.setLong(1, campaignId);
                        ps.setBigDecimal(2, orderedPlayerIds.get(i));
                    }

                    @Override
                    public int getBatchSize() {
                        return playerIds.size();
                    }
                });
    }

    @Override
    public int fetchCampaignTargets(final Long campaignId,
                                    final BatchVisitor<PlayerWithContent> visitor) {
        notNull(campaignId, "campaignId may not be null");
        notNull(visitor, "visitor may not be null");

        return template.query("SELECT DISTINCT PLAYER_ID FROM CAMPAIGN_TARGET WHERE CAMPAIGN_ID = ?",
                new BatchResultSetExtractor<>(new PlayerWithContentRowMapper(), visitor, batchSize()),
                campaignId);
    }

    @Override
    public Integer numberOfTargetsInCampaign(final Long campaignId) {
        return template.queryForInt("SELECT COUNT(*) from CAMPAIGN_TARGET WHERE CAMPAIGN_ID = ?", campaignId);
    }

    private static class PlayerWithContentRowMapper implements RowMapper<PlayerWithContent> {
        @Override
        public PlayerWithContent mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new PlayerWithContent(rs.getBigDecimal(1));
        }
    }

    private int batchSize() {
        return yazinoConfiguration.getInt(PROPERTY_BATCH_SIZE, DEFAULT_BATCH_SIZE);
    }
}
