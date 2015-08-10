package com.yazino.promotions;

import com.google.common.base.Splitter;
import com.yazino.platform.community.PaymentPreferences;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.yazino.promotions.PromotionConfigKeyEnum.*;
import static org.apache.commons.lang3.Validate.notNull;

@Repository(value = "buyChipsPromotionDefinitionDao")
public class MysqlBuyChipsPromotionDefinitionDao extends MysqlPromotionDefinitionDao<BuyChipsForm> {

    private final PromotionChipsPercentageDao promotionChipsPercentageDao;

    @Autowired
    public MysqlBuyChipsPromotionDefinitionDao(@Qualifier("dwNamedJdbcTemplate") final NamedParameterJdbcTemplate template,
                                               PromotionChipsPercentageDao promotionChipsPercentageDao) {
        super(template);
        notNull(promotionChipsPercentageDao);
        this.promotionChipsPercentageDao = promotionChipsPercentageDao;
    }

    @Override
    protected void saveFurtherInformation(final BuyChipsForm form, final Long promoDefinitionId) {
        promotionChipsPercentageDao.save(promoDefinitionId, form.getChipsPackagePercentages());
    }

    @Override
    protected void updateFurtherPromotionConfig(final BuyChipsForm form) {
        promotionChipsPercentageDao.updateChipsPercentage(form.getPromotionDefinitionId(), form.getChipsPackagePercentages());
    }

    @Override
    protected void updateFromPromotionConfig(final BuyChipsForm promoForm) {
        Map<Integer, BigDecimal> chipsPackagePercentages = promotionChipsPercentageDao.getChipsPercentages(promoForm.getPromotionDefinitionId());
        promoForm.setChipsPackagePercentages(chipsPackagePercentages);
        final HashMap<String, String> promotionConfig = fetchPromotionConfigById(promoForm.getPromotionDefinitionId());
        promoForm.setInGameNotificationHeader(promotionConfig.get(IN_GAME_NOTIFICATION_HEADER_KEY.getDescription()));
        promoForm.setInGameNotificationMsg(promotionConfig.get(IN_GAME_NOTIFICATION_MSG_KEY.getDescription()));
        promoForm.setRolloverHeader(promotionConfig.get(ROLLOVER_HEADER_KEY.getDescription()));
        promoForm.setRolloverText(promotionConfig.get(ROLLOVER_TEXT_KEY.getDescription()));
        promoForm.setMaxRewards(Integer.valueOf(promotionConfig.get(MAX_REWARDS_KEY.getDescription())));
        promoForm.setAllPlayers(Boolean.parseBoolean(promotionConfig.get(ALL_PLAYERS.getDescription())));
        List<PaymentPreferences.PaymentMethod> paymentMethods = getPaymentMethodsFromConfig(promotionConfig);
        promoForm.setPaymentMethods(paymentMethods);
    }

    @Override
    protected SqlParameterSource[] getInsertCampaignPromoConfigQueryParams(final BuyChipsForm buyChipsForm, final Long promoDefinitionId) {
        List<SqlParameterSource> queryParams = newArrayList();

        queryParams.add(getQueryParam(promoDefinitionId, IN_GAME_NOTIFICATION_HEADER_KEY.getDescription(), buyChipsForm.getInGameNotificationHeader()));
        queryParams.add(getQueryParam(promoDefinitionId, IN_GAME_NOTIFICATION_MSG_KEY.getDescription(), buyChipsForm.getInGameNotificationMsg()));
        queryParams.add(getQueryParam(promoDefinitionId, ROLLOVER_HEADER_KEY.getDescription(), buyChipsForm.getRolloverHeader()));
        queryParams.add(getQueryParam(promoDefinitionId, ROLLOVER_TEXT_KEY.getDescription(), buyChipsForm.getRolloverText()));
        queryParams.add(getQueryParam(promoDefinitionId, MAX_REWARDS_KEY.getDescription(), buyChipsForm.getMaxRewards().toString()));
        queryParams.add(getQueryParam(promoDefinitionId, ALL_PLAYERS.getDescription(), Boolean.toString(buyChipsForm.isAllPlayers())));
        queryParams.add(getQueryParam(promoDefinitionId, PAYMENT_METHODS.getDescription(), COMMA_JOINER.join(buyChipsForm.getPaymentMethods())));
        return queryParams.toArray(new SqlParameterSource[queryParams.size()]);
    }

    private List<PaymentPreferences.PaymentMethod> getPaymentMethodsFromConfig(HashMap<String, String> promotionConfig) {
        Iterable<String> paymentMethodsAsString = Splitter.on(",").split(promotionConfig.get(PAYMENT_METHODS.getDescription()));
        List<PaymentPreferences.PaymentMethod> paymentMethods = new ArrayList<PaymentPreferences.PaymentMethod>();
        for (String paymentMethodAsString : paymentMethodsAsString) {
            paymentMethods.add(PaymentPreferences.PaymentMethod.valueOf(paymentMethodAsString));
        }
        return paymentMethods;
    }

    @Override
    protected BuyChipsForm getNewForm() {
        return new BuyChipsForm();
    }
}
