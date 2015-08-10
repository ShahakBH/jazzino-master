package com.yazino.engagement.campaign.reporting.dao;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.reporting.domain.CampaignNotificationAuditMessage;
import com.yazino.engagement.campaign.reporting.domain.NotificationAuditType;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.engagement.ChannelType.IOS;
import static com.yazino.engagement.campaign.reporting.domain.NotificationAuditType.SEND_ATTEMPT;
import static com.yazino.engagement.campaign.reporting.domain.NotificationAuditType.SEND_SUCCESSFUL;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

@Transactional
@DirtiesContext
@ContextConfiguration
@TransactionConfiguration(transactionManager = "externalDwTransactionManager")
@RunWith(SpringJUnit4ClassRunner.class)
public class PsqlCampaignNotificationAuditDaoIntegrationTest {

    public static final DateTime CURRENT_DATE_TIME = new DateTime();
    public static final long CAMPAIGN_RUN_ID = 123L;
    public static final BigDecimal PLAYER_ID = new BigDecimal("123.11");

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private CampaignNotificationAuditDao underTest;

    @Before
    public void setUp() throws Exception {
        cleanupData();
        underTest = new PsqlCampaignNotificationAuditDao(jdbcTemplate);
    }

    private void cleanupData() {
        jdbcTemplate.update("DELETE FROM CAMPAIGN_NOTIFICATION_AUDIT");
    }


    @Test
    public void persistShouldInsertValuesToDb() {
        CampaignNotificationAuditMessage message1 =
                new CampaignNotificationAuditMessage(CAMPAIGN_RUN_ID, PLAYER_ID, IOS, "SLOTS", SEND_ATTEMPT, CURRENT_DATE_TIME);

        CampaignNotificationAuditMessage message2 =
                new CampaignNotificationAuditMessage(CAMPAIGN_RUN_ID, PLAYER_ID, IOS, "SLOTS", SEND_SUCCESSFUL, CURRENT_DATE_TIME);

        Set<CampaignNotificationAuditMessage> campaignNotificationMessages = newHashSet(message1, message2);
        underTest.persist(campaignNotificationMessages);

        List<CampaignNotificationAuditMessage> messagesFromDb = getDataFromDb();

        assertThat(messagesFromDb, hasItems(message1, message2));

    }

    private List<CampaignNotificationAuditMessage> getDataFromDb() {
        return jdbcTemplate.query("SELECT * FROM CAMPAIGN_NOTIFICATION_AUDIT WHERE CAMPAIGN_RUN_ID=" + CAMPAIGN_RUN_ID,
                new RowMapper<CampaignNotificationAuditMessage>() {
                    @Override
                    public CampaignNotificationAuditMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new CampaignNotificationAuditMessage(rs.getLong("CAMPAIGN_RUN_ID"),
                                rs.getBigDecimal("PLAYER_ID"),
                                ChannelType.valueOf(rs.getString("CHANNEL")),
                                rs.getString("GAME_TYPE"),
                                NotificationAuditType.valueOf(rs.getString("NOTIFICATION_AUDIT_TYPE")),
                                new DateTime(rs.getTimestamp("RUN_TS")));
                    }
                });
    }
}
