package com.yazino.bi.operations.view;

import strata.server.lobby.api.promotion.PromotionType;

import java.util.Map;

public class SearchForm {
    private PromotionType promotionType;
    private PromotionSearchType searchType;
    private Map<PromotionSearchType, String> searchOptions;
    private Map<PromotionType, String> promotionTypes;

    public PromotionType getPromotionType() {
        return promotionType;
    }

    public void setPromotionType(final PromotionType promotionType) {
        this.promotionType = promotionType;
    }

    public PromotionSearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(final PromotionSearchType searchType) {
        this.searchType = searchType;
    }

    public Map<PromotionSearchType, String> getSearchTypes() {
        return searchOptions;
    }

    public void setSearchTypes(final Map<PromotionSearchType, String> searchTypes) {
        this.searchOptions = searchTypes;
    }

    public Map<PromotionType, String> getPromotionTypes() {
        return promotionTypes;
    }

    public void setPromotionTypes(final Map<PromotionType, String> promotionTypes) {
        this.promotionTypes = promotionTypes;
    }
}
