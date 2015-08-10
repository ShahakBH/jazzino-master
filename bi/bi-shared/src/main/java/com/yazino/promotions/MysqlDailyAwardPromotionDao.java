package com.yazino.promotions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.yazino.promotions.PromotionConfigKeyEnum.*;

@Repository(value = "dailyAwardPromotionDefinitionDao")
public class MysqlDailyAwardPromotionDao extends MysqlPromotionDefinitionDao<DailyAwardForm> {

    @Autowired
    public MysqlDailyAwardPromotionDao(@Qualifier("dwNamedJdbcTemplate") final NamedParameterJdbcTemplate template) {
        super(template);
    }

    @Override
    protected void updateFromPromotionConfig(final DailyAwardForm dailyAwardForm) {
        final HashMap<String, String> promotionConfig = fetchPromotionConfigById(dailyAwardForm.getPromotionDefinitionId());
        dailyAwardForm.setTopUpAmount(Integer.valueOf(promotionConfig.get(TOPUP_AMOUNT_KEY.getDescription())));
        dailyAwardForm.setMaxRewards(Integer.valueOf(promotionConfig.get(MAX_REWARDS_KEY.getDescription())));
        dailyAwardForm.setAllPlayers(Boolean.parseBoolean(promotionConfig.get(ALL_PLAYERS.getDescription())));
    }

    @Override
    protected DailyAwardForm getNewForm() {
        return new DailyAwardForm();
    }

    @Override
    protected SqlParameterSource[] getInsertCampaignPromoConfigQueryParams(final DailyAwardForm dailyAwardForm, final Long promoDefinitionId) {
        List<SqlParameterSource> queryParams = new ArrayList<SqlParameterSource>();

        queryParams.add(getQueryParam(promoDefinitionId, TOPUP_AMOUNT_KEY.getDescription(), dailyAwardForm.getTopUpAmount().toString()));
        queryParams.add(getQueryParam(promoDefinitionId, MAX_REWARDS_KEY.getDescription(), dailyAwardForm.getMaxRewards().toString()));
        queryParams.add(getQueryParam(promoDefinitionId, ALL_PLAYERS.getDescription(), Boolean.toString(dailyAwardForm.isAllPlayers())));


        return queryParams.toArray(new SqlParameterSource[queryParams.size()]);
    }


    @Override
    protected void saveFurtherInformation(final DailyAwardForm form, final Long promoDefinitionId) {

    }

    @Override
    protected void updateFurtherPromotionConfig(final DailyAwardForm form) {

    }

}
