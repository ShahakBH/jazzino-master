package com.yazino.bi.operations.persistence.facebook;

import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.util.Date;
import java.util.Map;

/**
 * Ads API service for Facebook
 */
public interface FacebookAdApiService {

    /**
     * Gets the map of ad statistics per ad group
     *
     * @param startDate Lower limit of the ad group
     * @param endDate   Upper limit of the ad group
     * @return Ad name - to - stats map for each campaign involved
     */
    Map<String, FacebookAdsStatsData> getAdGroupStats(Date startDate, Date endDate)
            throws FacebookAdApiException;

}
