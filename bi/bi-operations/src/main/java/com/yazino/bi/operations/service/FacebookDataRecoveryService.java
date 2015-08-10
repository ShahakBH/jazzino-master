package com.yazino.bi.operations.service;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.bi.operations.persistence.FacebookDataRecordingDao;
import com.yazino.bi.operations.persistence.facebook.FacebookAdApiException;
import com.yazino.bi.operations.persistence.facebook.FacebookAdApiService;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service("facebookDataRecoveryService")
public class FacebookDataRecoveryService {

    private static final Logger LOG = LoggerFactory.getLogger(FacebookDataRecoveryService.class);
    public static final int YEAR_2012 = 2012;
    public static final int MONTH_OF_YEAR = 6;
    public static final int DAY_OF_MONTH = 1;
    public static final Date DEFAULT_DATE = new DateTime(YEAR_2012, MONTH_OF_YEAR, DAY_OF_MONTH, 0, 0, 0, 0).toDate();

    @Autowired
    @Qualifier("dwFacebookDataRecordingDao")
    private FacebookDataRecordingDao dwDao;

    @Autowired
    @Qualifier("externalDwFacebookDataRecordingDao")
    private FacebookDataRecordingDao externalDwDao;

    @Autowired
    private FacebookAdApiService adApi;

    public void fillFacebookData() {
        try {
            Date lastRecordDate = getLastRecordDate();
            final Date now = new DateTime().toDate();
            fillFacebookDataToDb(lastRecordDate, now);
        } catch (Exception e) {
            LOG.error("Cron Job for Recovering facebook Data failed", e);
        }
    }

    private void fillFacebookDataToDb(Date fromDate, Date toDate) {
        while (fromDate.getTime() < toDate.getTime()) {
            final Date nextDate = new DateTime(fromDate).plusDays(1).toDate();
            LOG.debug("Pre: " + fromDate + " - " + nextDate);
            try {
                final Map<String, FacebookAdsStatsData> adGroupStats = adApi.getAdGroupStats(fromDate, nextDate);
                if (adGroupStats != null && !(adGroupStats.isEmpty())) {
                    saveDataToDb(fromDate, adGroupStats);
                }

            } catch (final FacebookAdApiException e) {
                LOG.error("Facebook Ad Api error", e);
            }
            fromDate = nextDate;
            LOG.debug("Running for " + fromDate);
        }
    }

    private void saveDataToDb(Date lastRecordDate, Map<String, FacebookAdsStatsData> adGroupStats) {
        dwDao.saveFacebookData(lastRecordDate, adGroupStats);
        externalDwDao.saveFacebookData(lastRecordDate, adGroupStats);
    }

    private Date getLastRecordDate() {
        Date lastRecordDate = dwDao.getLatestRecordDate();
        if (lastRecordDate == null) {
            lastRecordDate = DEFAULT_DATE;
        }
        return lastRecordDate;
    }

}
