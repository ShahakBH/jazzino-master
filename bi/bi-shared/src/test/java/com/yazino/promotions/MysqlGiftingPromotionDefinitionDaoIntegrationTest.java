package com.yazino.promotions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static utils.ParamBuilder.params;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
@DirtiesContext
public class MysqlGiftingPromotionDefinitionDaoIntegrationTest {

    @Autowired
    @Qualifier("dwNamedJdbcTemplate")
    private NamedParameterJdbcTemplate template;

    private GiftingPromotionDefinitionDao underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new GiftingPromotionDefinitionDao(template);

    }

    @Test
    public void promoDefinitionDaoShouldSaveAndLoadPromoDefinition() {
        final GiftingForm giftingForm = new GiftingForm();
        giftingForm.setAllPlayers(true);
        giftingForm.setDescription("your mum");
        giftingForm.setTitle("I want");
        giftingForm.setReward(999L);
        giftingForm.setName("HARRO");
        giftingForm.setGameType("SLOTS");
        giftingForm.setPriority(99);
        giftingForm.setPlatforms(null);
        giftingForm.setValidForHours(24);
        giftingForm.setCampaignId(666L);

        final Long promoDefId = underTest.save(giftingForm);
        final List<Map<String, Object>> promos = template.queryForList("select * From PROMOTION_DEFINITION where id=:promoId", params().promoId(promoDefId));
        final List<Map<String, Object>> promosConfigs = template.queryForList("select * From CAMPAIGN_PROMOTION_CONFIG where promotion_definition_id=:promoId", params().promoId(promoDefId));
        System.out.println(promos);
        System.out.println(promosConfigs);

        final GiftingForm loadedGiftingForm = underTest.getForm(666L);
        giftingForm.setPromotionDefinitionId(loadedGiftingForm.getPromotionDefinitionId());
        assertThat(loadedGiftingForm, is(equalTo(giftingForm)));
    }

    @Test
    public void updateShouldModifyPromoDefinition() {
        final GiftingForm giftingForm = new GiftingForm();
        giftingForm.setCampaignId(666L);
        giftingForm.setReward(123L);
        final Long promoDefId = underTest.save(giftingForm);

        giftingForm.setAllPlayers(true);
        giftingForm.setDescription("your mum");
        giftingForm.setTitle("I want");
        giftingForm.setReward(999L);
        giftingForm.setName("HARRO");
        giftingForm.setGameType("SLOTS");
        giftingForm.setPriority(99);
        giftingForm.setPlatforms(null);
        giftingForm.setValidForHours(24);
        giftingForm.setCampaignId(666L);
        giftingForm.setPromotionDefinitionId(promoDefId);

        underTest.update(giftingForm);

        final GiftingForm loadedGiftingForm = underTest.getForm(666L);

        assertThat(loadedGiftingForm, is(equalTo(giftingForm)));
    }
}
