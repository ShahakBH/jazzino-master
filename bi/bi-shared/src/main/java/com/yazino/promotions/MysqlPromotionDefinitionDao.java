package com.yazino.promotions;

import com.google.common.base.Joiner;
import com.yazino.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import strata.server.lobby.api.promotion.PromotionType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class MysqlPromotionDefinitionDao<T extends PromotionForm> implements PromotionFormDefinitionDao<T> {

    protected static final Joiner COMMA_JOINER = Joiner.on(",").skipNulls();

    private static final Logger LOG = LoggerFactory.getLogger(MysqlPromotionDefinitionDao.class);
    private NamedParameterJdbcTemplate template;

    private static final String REPLACE_PROMO_CONFIG = "REPLACE INTO CAMPAIGN_PROMOTION_CONFIG (PROMOTION_DEFINITION_ID, CONFIG_KEY, CONFIG_VALUE) VALUES "
            + "(:promotionDefinitionId, :configKey, :configValue)";

    public MysqlPromotionDefinitionDao(final NamedParameterJdbcTemplate template) {
        notNull(template);
        this.template = template;
    }

    @Override
    public final Long save(final T form) {
        Long promoDefinitionId = insertIntoPromoDefinition(form);
        insertIntoCampaignPromotionConfig(form, promoDefinitionId);
        saveFurtherInformation(form, promoDefinitionId);
        return promoDefinitionId;
    }

    @Override
    public final void update(final T dailyAwardForm) {
        if (dailyAwardForm.getPromotionDefinitionId() == null) {
            save(dailyAwardForm);
            return;
        }
        MapSqlParameterSource promoDefParams = mapBaseFormToPromoDefParams(dailyAwardForm);

        template.update("UPDATE PROMOTION_DEFINITION SET CAMPAIGN_ID=:campaignId, NAME=:name, VALID_FOR_HOURS=:validForHours, "
                + "PRIORITY=:priority, PLATFORMS=:platforms, PROMOTION_TYPE=:promoType WHERE ID=:id", promoDefParams);
        insertUpdatePromoConfig(dailyAwardForm);
    }

    @Override
    public final T getForm(final Long campaignId) {
        final T promoForm = getPromoFormFromDefinition(campaignId);
        LOG.debug("BuyChipsForm {} retrieved for campaign: {}", promoForm, campaignId);
        updateFromPromotionConfig(promoForm);
        return promoForm;
    }

    protected abstract void saveFurtherInformation(final T form, final Long promoDefinitionId);

    protected abstract void updateFurtherPromotionConfig(final T form);

    protected abstract SqlParameterSource[] getInsertCampaignPromoConfigQueryParams(final T form, final Long promoDefinitionId);

    protected abstract T getNewForm();

    protected abstract void updateFromPromotionConfig(final T promoForm);

    protected T getPromoFormFromDefinition(final Long campaignId) {
        final HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("campaignId", campaignId);

        return template.queryForObject(
                "SELECT ID, CAMPAIGN_ID, NAME, VALID_FOR_HOURS, PRIORITY, PLATFORMS, PROMOTION_TYPE FROM PROMOTION_DEFINITION WHERE CAMPAIGN_ID = :campaignId",
                paramMap,
                new RowMapper<T>() {
                    @Override
                    public T mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return promoFormRowMapper(rs, getNewForm());
                    }
                }
        );
    }

    private Long insertIntoPromoDefinition(final T promotionForm) {
        final GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource promoDefParams = mapBaseFormToPromoDefParams(promotionForm);

        template.update(
                "INSERT INTO PROMOTION_DEFINITION (CAMPAIGN_ID, NAME, VALID_FOR_HOURS, PRIORITY, PLATFORMS, PROMOTION_TYPE) "
                        + "VALUES (:campaignId, :name, :validForHours, :priority, :platforms, :promoType)",
                promoDefParams, keyHolder
        );

        return keyHolder.getKey().longValue();
    }

    private void insertUpdatePromoConfig(final T form) {
        final SqlParameterSource[] promoConfigArgs = getInsertCampaignPromoConfigQueryParams(form, form.getPromotionDefinitionId());
        template.batchUpdate(REPLACE_PROMO_CONFIG, promoConfigArgs);
        updateFurtherPromotionConfig(form);
    }

    private void insertIntoCampaignPromotionConfig(final T form, final Long promoDefinitionId) {
        final SqlParameterSource[] promoConfigArgs = getInsertCampaignPromoConfigQueryParams(form, promoDefinitionId);

        insertIntoCampaignPromotionConfig(promoConfigArgs);
    }

    private int[] insertIntoCampaignPromotionConfig(final SqlParameterSource[] promoConfigArgs) {
        return template.batchUpdate(
                "INSERT INTO CAMPAIGN_PROMOTION_CONFIG (PROMOTION_DEFINITION_ID, CONFIG_KEY, CONFIG_VALUE) VALUES (:promotionDefinitionId, :configKey, :configValue)",
                promoConfigArgs);
    }


    private T promoFormRowMapper(final ResultSet rs, T promotionForm) throws SQLException {

        promotionForm.setPromotionDefinitionId(rs.getLong("ID"));
        promotionForm.setCampaignId(rs.getLong("CAMPAIGN_ID"));
        promotionForm.setName(rs.getString("NAME"));
        promotionForm.setValidForHours(rs.getInt("VALID_FOR_HOURS"));
        promotionForm.setPriority(rs.getInt("PRIORITY"));
        promotionForm.setPromoType(PromotionType.valueOf(rs.getString("PROMOTION_TYPE")));

        getPlatformListFromString(rs.getString("PLATFORMS"), promotionForm);
        return promotionForm;
    }

    private MapSqlParameterSource mapBaseFormToPromoDefParams(final T promotionForm) {
        MapSqlParameterSource promoDefParams = new MapSqlParameterSource();
        promoDefParams.addValue("id", promotionForm.getPromotionDefinitionId());
        promoDefParams.addValue("campaignId", promotionForm.getCampaignId());
        promoDefParams.addValue("name", promotionForm.getName());
        promoDefParams.addValue("validForHours", promotionForm.getValidForHours());
        promoDefParams.addValue("priority", promotionForm.getPriority());
        promoDefParams.addValue("platforms", getPlatformListAsString(promotionForm.getPlatforms()));
        promoDefParams.addValue("promoType", promotionForm.getPromoType().name());
        if (promotionForm.getPromotionDefinitionId() != null) {
            promoDefParams.addValue("id", promotionForm.getPromotionDefinitionId());
        }
        return promoDefParams;
    }

    protected final MapSqlParameterSource getQueryParam(final Long promoDefinitionId, final String configKey, final String configValue) {
        MapSqlParameterSource promoConfigParams = new MapSqlParameterSource();
        promoConfigParams.addValue("promotionDefinitionId", promoDefinitionId);
        promoConfigParams.addValue("configKey", configKey);
        promoConfigParams.addValue("configValue", configValue);

        return promoConfigParams;
    }

    protected final HashMap<String, String> fetchPromotionConfigById(final Long promotionDefinitionId) {
        final HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("promotionDefinitionId", promotionDefinitionId);

        return template.query(
                "SELECT CONFIG_KEY, CONFIG_VALUE FROM CAMPAIGN_PROMOTION_CONFIG WHERE PROMOTION_DEFINITION_ID = :promotionDefinitionId",
                paramMap,
                new ResultSetExtractor<HashMap<String, String>>() {
                    @Override
                    public HashMap<String, String> extractData(final ResultSet rs) throws SQLException, DataAccessException {
                        final HashMap<String, String> contentMap = new HashMap<>();
                        while (rs.next()) {
                            contentMap.put(rs.getString("CONFIG_KEY"), rs.getString("CONFIG_VALUE"));
                        }
                        return contentMap;
                    }
                }
        );
    }

    void getPlatformListFromString(final String platformsString, final PromotionForm promotionForm) throws SQLException {
        if (platformsString != null) {
            final String[] platformArray = platformsString.split(",");
            final List<Platform> platformList = new ArrayList<>();

            for (String platformString : platformArray) {
                platformList.add(Platform.valueOf(platformString));
            }

            promotionForm.setPlatforms(platformList);
        } else {
            promotionForm.setPlatforms(null);

        }
    }

    String getPlatformListAsString(List<Platform> platforms) {
        if (platforms == null) {
            return null;
        }
        return COMMA_JOINER.join(platforms);
    }

}
