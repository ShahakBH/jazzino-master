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

@Repository
public class GiftingPromotionDefinitionDao extends MysqlPromotionDefinitionDao<GiftingForm> {


    @Autowired
    public GiftingPromotionDefinitionDao(@Qualifier("dwNamedJdbcTemplate") final NamedParameterJdbcTemplate template) {
        super(template);
    }


    @Override
    protected void updateFromPromotionConfig(final GiftingForm promoForm) {
        final HashMap<String, String> promoConfig = fetchPromotionConfigById(promoForm.getPromotionDefinitionId());
        promoForm.setDescription(promoConfig.get(GIFT_DESCRIPTION_KEY.getDescription()));
        promoForm.setTitle(promoConfig.get(GIFT_TITLE_KEY.getDescription()));
        promoForm.setGameType(promoConfig.get(GAME_TYPE.getDescription()));
        final String allPlayers = promoConfig.get(ALL_PLAYERS.getDescription());
        promoForm.setAllPlayers(allPlayers == null ? false : Boolean.valueOf(allPlayers));
        final String reward = promoConfig.get(REWARD_CHIPS_KEY.getDescription());
        promoForm.setReward(reward == null ? null : Long.valueOf(reward));
    }

    @Override
    protected SqlParameterSource[] getInsertCampaignPromoConfigQueryParams(final GiftingForm form, final Long promoDefinitionId) {
        List<SqlParameterSource> queryParams = new ArrayList<SqlParameterSource>();
        queryParams.add(getQueryParam(promoDefinitionId, GIFT_DESCRIPTION_KEY.getDescription(), form.getDescription()));
        queryParams.add(getQueryParam(promoDefinitionId, GIFT_TITLE_KEY.getDescription(), form.getTitle()));
        queryParams.add(getQueryParam(promoDefinitionId, REWARD_CHIPS_KEY.getDescription(), form.getReward().toString()));
        queryParams.add(getQueryParam(promoDefinitionId, GAME_TYPE.getDescription(), form.getGameType()));
        queryParams.add(getQueryParam(promoDefinitionId, ALL_PLAYERS.getDescription(), Boolean.toString(form.isAllPlayers())));

        return queryParams.toArray(new SqlParameterSource[queryParams.size()]);
    }


    @Override
    protected void saveFurtherInformation(final GiftingForm form, final Long promoDefinitionId) {
        //nothing
    }

    @Override
    protected void updateFurtherPromotionConfig(final GiftingForm form) {
        //nothing
    }

    @Override
    protected GiftingForm getNewForm() {
        return new GiftingForm();
    }

}
