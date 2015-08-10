package com.yazino.bi.operations.util;


import org.joda.time.DateTime;
import com.yazino.bi.operations.persistence.FacebookDataRecordingDao;
import com.yazino.bi.operations.persistence.facebook.FacebookAdApiService;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.util.Map;

public class RecoveryService {
    private FacebookDataRecordingDao dao;
    private FacebookAdApiService adApi;

    public RecoveryService(FacebookDataRecordingDao dao, FacebookAdApiService adApi) {
        this.dao = dao;
        this.adApi = adApi;
    }

    public void recover(DateTime startDate, DateTime endDate) {

        try {
            final Map<String, FacebookAdsStatsData> adGroupStats = adApi
                    .getAdGroupStats(startDate.toDate(), endDate.toDate());

            dao.saveFacebookData(startDate.toDate(), adGroupStats);

            System.out.println("got data for: " + startDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
