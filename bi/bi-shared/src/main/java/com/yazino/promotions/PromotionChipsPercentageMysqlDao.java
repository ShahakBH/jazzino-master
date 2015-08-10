package com.yazino.promotions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class PromotionChipsPercentageMysqlDao implements PromotionChipsPercentageDao {

    public static final String GET_CHIP_PERCENTAGES = "SELECT PACKAGE_NUMBER, PERCENTAGE FROM PROMOTION_CHIPS_PERCENTAGE WHERE PROMOTION_DEFINITION_ID = "
            + ":promotionDefinitionId ORDER BY PACKAGE_NUMBER";
    public static final String ADD_CHIP_PERCENTAGES = "INSERT INTO PROMOTION_CHIPS_PERCENTAGE (PROMOTION_DEFINITION_ID, PACKAGE_NUMBER, PERCENTAGE) VALUES "
            + "(:promotionDefinitionId, :packageNumber, :percentage)";
    public static final String REPLACE_PROMO_CHIPS_PERCENTAGE = " REPLACE INTO PROMOTION_CHIPS_PERCENTAGE (PROMOTION_DEFINITION_ID, PACKAGE_NUMBER, PERCENTAGE)"
            + " VALUES (:promotionDefinitionId, :packageNumber, :percentage)";
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public PromotionChipsPercentageMysqlDao(@Qualifier("dwNamedJdbcTemplate") final NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public Map<Integer, BigDecimal> getChipsPercentages(Long promotionDefinitionId) {
        final HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("promotionDefinitionId", promotionDefinitionId);
        return template.query(GET_CHIP_PERCENTAGES, paramMap,
                new ResultSetExtractor<Map<Integer, BigDecimal>>() {
                    @Override
                    public Map<Integer, BigDecimal> extractData(ResultSet rs) throws SQLException, DataAccessException {
                        Map<Integer, BigDecimal> chipsPercentages = new LinkedHashMap<Integer, BigDecimal>();
                        while (rs.next()) {
                            chipsPercentages.put(rs.getInt("PACKAGE_NUMBER"), rs.getBigDecimal("PERCENTAGE"));
                        }
                        return chipsPercentages;
                    }
                });
    }

    @Override
    public void save(Long promotionDefinitionId, Map<Integer, BigDecimal> chipsPercentagePackages) {
        for (Integer packageIndex : chipsPercentagePackages.keySet()) {
            final HashMap<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put("promotionDefinitionId", promotionDefinitionId);
            paramMap.put("packageNumber", packageIndex);
            paramMap.put("percentage", chipsPercentagePackages.get(packageIndex));
            template.update(ADD_CHIP_PERCENTAGES, paramMap);
        }

    }

    @Override
    public void updateChipsPercentage(Long promotionDefinitionId, Map<Integer, BigDecimal> chipsPackagePercentages) {

        final SqlParameterSource[] promoConfigArgs = getPromoPercentageQueryParams(promotionDefinitionId, chipsPackagePercentages);

        // Remember that replace needs unique index on that table to work as expected
        template.batchUpdate(REPLACE_PROMO_CHIPS_PERCENTAGE, promoConfigArgs);

    }

    private SqlParameterSource[] getPromoPercentageQueryParams(Long promotionDefinitionId, Map<Integer, BigDecimal> chipsPackagePercentages) {
        List<SqlParameterSource> queryParams = new ArrayList<SqlParameterSource>();
        for (Integer packageIndex : chipsPackagePercentages.keySet()) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("promotionDefinitionId", promotionDefinitionId);
            params.addValue("packageNumber", packageIndex);
            params.addValue("percentage", chipsPackagePercentages.get(packageIndex));
            queryParams.add(params);
        }
        return queryParams.toArray(new SqlParameterSource[queryParams.size()]);
    }
}
