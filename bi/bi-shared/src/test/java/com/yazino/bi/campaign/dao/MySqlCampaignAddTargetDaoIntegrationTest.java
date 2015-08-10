package com.yazino.bi.campaign.dao;

import com.yazino.bi.campaign.domain.CampaignDefinition;
import com.yazino.bi.persistence.BatchVisitor;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static java.math.BigDecimal.valueOf;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class MySqlCampaignAddTargetDaoIntegrationTest {

    @Autowired
    private YazinoConfiguration yazinoConfiguration;
    @Autowired
    private JdbcTemplate template;
    @Autowired
    private CampaignDefinitionDao campaignDefinitionDao;

    private CampaignAddTargetDao underTest;
    private Long campaignId;

    @Before
    public void setUp() throws Exception {
        campaignId = campaignDefinitionDao.save(new CampaignDefinition(null,
                "name",
                "select 1",
                new HashMap<String, String>(),
                null,
                false,
                null,
                true,
                false));
        template.update("DELETE FROM CAMPAIGN_TARGET WHERE campaign_id = ?", campaignId);
        underTest = new MySqlCampaignAddTargetDao(template, yazinoConfiguration);
    }

    @Test
    public void savePlayersShouldWritePlayersToCampaignTargetTable() {

        final Set<BigDecimal> playerIds = new LinkedHashSet<>();
        final List<BigDecimal> playerIdList = getPlayerIdList();
        playerIds.addAll(playerIdList);

        underTest.savePlayersToCampaign(campaignId, playerIds);

        assertThat(getPlayerIdsForCampaign(campaignId),
                CoreMatchers.is(IsEqual.equalTo(playerIdList)));
    }

    @Test
    public void savePlayersShouldIgnoreDuplicatePlayersToCampaignTargetTable() {

        final Set<BigDecimal> playerIds = new LinkedHashSet<>();
        final List<BigDecimal> playerIdList = getPlayerIdList();
        playerIds.addAll(playerIdList);

        underTest.savePlayersToCampaign(campaignId, playerIds);
        underTest.savePlayersToCampaign(campaignId, playerIds);

        assertThat(getPlayerIdsForCampaign(campaignId),
                CoreMatchers.is(IsEqual.equalTo(playerIdList)));
    }

    private List<BigDecimal> getPlayerIdList() {
        return asList(
                valueOf(-1l),
                valueOf(-2l),
                valueOf(-3l),
                valueOf(-4l));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fetchCampaignTargetsShouldNotCallVisitorIfNoResults() {
        final BatchVisitor<PlayerWithContent> visitor = mock(BatchVisitor.class);

        MatcherAssert.assertThat(underTest.fetchCampaignTargets(213213l, visitor), is(equalTo(0)));
        verifyZeroInteractions(visitor);
    }

    @Test
    public void fetchCampaignTargetsShouldInvokeVisitorWithPlayers() {
        final Set<BigDecimal> playerIds = new LinkedHashSet<>();
        final List<BigDecimal> playerIdList = getPlayerIdList();
        playerIds.addAll(playerIdList);
        final List<PlayerWithContent> passedItems = new ArrayList<>();
        underTest.savePlayersToCampaign(campaignId, playerIds);

        final int targetCount = underTest.fetchCampaignTargets(campaignId, new BatchVisitor<PlayerWithContent>() {
            @Override
            public void processBatch(final List<PlayerWithContent> batch) {
                passedItems.addAll(batch);
            }
        });

        assertThat(targetCount, is(equalTo(4)));
        assertThat(passedItems, hasItems(
                new PlayerWithContent(valueOf(-1l)),
                new PlayerWithContent(valueOf(-2l)),
                new PlayerWithContent(valueOf(-3l)),
                new PlayerWithContent(valueOf(-4l))));
    }

    @Test
    public void numberOfCampaignTargetsShouldNumberOfTargets() {
        final Set<BigDecimal> playerIds = new LinkedHashSet<>();
        final List<BigDecimal> playerIdList = getPlayerIdList();
        playerIds.addAll(playerIdList);

        underTest.savePlayersToCampaign(campaignId, playerIds);

        assertThat(underTest.numberOfTargetsInCampaign(campaignId), CoreMatchers.is(IsEqual.equalTo(4)));
    }

    private List<BigDecimal> getPlayerIdsForCampaign(final Long campaignId) {
        return template.query("SELECT PLAYER_ID FROM CAMPAIGN_TARGET WHERE CAMPAIGN_ID = ? order by PLAYER_ID DESC",
                new RowMapper<BigDecimal>() {
                    @Override
                    public BigDecimal mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return rs.getBigDecimal("player_id");
                    }
                },
                campaignId);
    }

}
