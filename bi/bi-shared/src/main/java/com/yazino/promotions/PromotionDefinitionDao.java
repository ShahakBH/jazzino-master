package com.yazino.promotions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.lobby.api.promotion.PromotionType;

import java.util.HashMap;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class PromotionDefinitionDao {

    private final NamedParameterJdbcTemplate template;
    private static final Logger LOG = LoggerFactory.getLogger(PromotionDefinitionDao.class);

    @Autowired
    public PromotionDefinitionDao(@Qualifier("dwNamedJdbcTemplate") final NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public PromotionType getPromotionDefinitionType(final long campaignId) {
        notNull(campaignId);
        final HashMap<String, Object> params = newHashMap();
        params.put("campaignId", campaignId);

        try {
            final String promoName = template.queryForObject("select PROMOTION_TYPE from PROMOTION_DEFINITION "
                    + "where campaign_id = :campaignId", params, String.class);
            try {
                return PromotionType.valueOf(promoName);
            } catch (IllegalArgumentException e) {
                LOG.warn("unknown promo type " + promoName + " for campaignId " + campaignId);
                throw new DataRetrievalFailureException("unknown promo type " + promoName + " for campaignId " + campaignId);
            }
        } catch (EmptyResultDataAccessException e) {
            return null;

        }
    }
}
