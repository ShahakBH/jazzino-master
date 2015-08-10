package com.yazino.bi.operations.persistence;

import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.util.Date;
import java.util.Map;

public interface FacebookDataRecordingDao {

    Date getLatestRecordDate();

    void saveFacebookData(final Date now, final Map<String, FacebookAdsStatsData> adGroupStats);
}
