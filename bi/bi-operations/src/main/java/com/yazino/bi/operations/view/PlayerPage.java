package com.yazino.bi.operations.view;

import strata.server.lobby.api.promotion.Promotion;
import com.yazino.bi.operations.model.PromotionPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlayerPage {
    private Promotion promotion;
    private int pageNumber;
    private int pagesAvailable;
    private int pageSize;
    private List<PromotionPlayer> promotionPlayers = new ArrayList<PromotionPlayer>();
    private PromotionSearchType searchType;

    public void setPageNumber(final int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public void setPagesAvailable(final int pagesAvailable) {
        this.pagesAvailable = pagesAvailable;
    }

    public void setPromotionPlayers(final List<PromotionPlayer> promotionPlayers) {
        this.promotionPlayers = promotionPlayers;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPagesAvailable() {
        return pagesAvailable;
    }

    public List<PromotionPlayer> getPromotionPlayers() {
        return promotionPlayers;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(final Promotion promotion) {
        this.promotion = promotion;
    }

    public PromotionSearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(final PromotionSearchType searchType) {
        this.searchType = searchType;
    }
}

