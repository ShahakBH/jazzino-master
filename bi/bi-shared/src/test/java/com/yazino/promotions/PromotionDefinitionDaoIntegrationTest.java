package com.yazino.promotions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import strata.server.lobby.api.promotion.PromotionType;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
@DirtiesContext
public class PromotionDefinitionDaoIntegrationTest {

    PromotionDefinitionDao underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new PromotionDefinitionDao(template);

    }

    @Autowired
    @Qualifier("dwNamedJdbcTemplate")
    private NamedParameterJdbcTemplate template;

    @Test
    public void getPromoTypeShouldLoadBuyChips() {
        final Map<String, Object> params = newHashMap();
        params.put("id", 1);
        params.put("campaignId", 10);
        params.put("name", "your mum");
        params.put("promoType", PromotionType.BUY_CHIPS.name());
        template.update("insert into PROMOTION_DEFINITION values(:id,:campaignId, :name, 1,1,'WEB',:promoType)", params);
        assertThat(underTest.getPromotionDefinitionType(10), equalTo(PromotionType.BUY_CHIPS));
    }

    @Test
    public void getPromoTypeShouldLoadDailyAward() {
        final Map<String, Object> params = newHashMap();
        params.put("id", 1);
        params.put("campaignId", 10);
        params.put("name", "your mum");
        params.put("promoType", PromotionType.DAILY_AWARD.name());
        template.update("insert into PROMOTION_DEFINITION values(:id,:campaignId, :name, 1,1,'WEB',:promoType)", params);
        assertThat(underTest.getPromotionDefinitionType(10), equalTo(PromotionType.DAILY_AWARD));
    }

    @Test(expected = DataRetrievalFailureException.class)
    public void getPromoTypeShouldFailIfCannotParsePromotionType() {
        final Map<String, Object> params = newHashMap();
        params.put("id", 1);
        params.put("campaignId", 10);
        params.put("name", "your mum");
        params.put("promoType", "unknown");
        template.update("insert into PROMOTION_DEFINITION values(:id,:campaignId, :name, 1,1,'WEB',:promoType)", params);
        underTest.getPromotionDefinitionType(10);
        fail();
    }

    @Test
    public void getPromoTypeShouldReturnNullIfNoPromotion() {
        assertNull(underTest.getPromotionDefinitionType(10));
    }


}
