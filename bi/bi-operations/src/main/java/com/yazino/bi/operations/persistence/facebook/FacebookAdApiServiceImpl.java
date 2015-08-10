package com.yazino.bi.operations.persistence.facebook;

import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.bi.operations.persistence.facebook.data.AdGroup;
import com.yazino.bi.operations.persistence.facebook.data.Campaign;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.util.*;

/**
 * Service giving access to the Facebook Ad API
 */
public class FacebookAdApiServiceImpl implements FacebookAdApiService {

    private static final Logger LOG = LoggerFactory.getLogger(FacebookAdApiServiceImpl.class);

    @Autowired
    private FacebookClientFactory clientFactory;

    private FacebookClient client = null;

    private String accessToken;

    private String adAccount;

    private int timeShift = 1;

    /**
     * Sets the time shift for the Ads API
     *
     * @param timeShift Time shift in hours
     */
    public void setTimeShift(final int timeShift) {
        this.timeShift = timeShift;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public void setAdAccount(final String adAccount) {
        this.adAccount = adAccount;
    }

    public void setClientFactory(final FacebookClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    /**
     * Initializes the access service
     */
    public void init() {
        if (client != null) {
            return;
        }
        client = clientFactory.createMapCapableGraphClient(accessToken);
    }

    /**
     * Gets the statistics for the given list of campaigns
     *
     * @param campaignIds List of IDS of the campaigns we're interested in
     * @param startDate   Start of the date range we wish to work with
     * @param endDate     End of the date range we wish to work with
     * @return Object holding the statistics of the listed campaigns
     */
    private List<FacebookAdsStatsData> getCampaignsStats(final List<String> campaignIds,
                                                         final Date startDate,
                                                         final Date endDate) {
        return client.fetchConnection("act_" + adAccount + "/adcampaignstats", FacebookAdsStatsData.class,
                Parameter.with("campaign_ids", campaignIds), Parameter.with("include_deleted", true),
                Parameter.with("start_time", startDate.getTime() / DateTimeConstants.MILLIS_PER_SECOND),
                Parameter.with("end_time", endDate.getTime() / DateTimeConstants.MILLIS_PER_SECOND)).getData();
    }

    private List<Campaign> getCampaignList() {
        return client.fetchConnection("act_" + adAccount + "/adcampaigns", Campaign.class,
                Parameter.with("offset", 0), Parameter.with("include_count", false),
                Parameter.with("fields", "account_id,id,name")).getData();
    }

    /**
     * Returns the list of all ad groups for the managed account
     *
     * @return List of campaigns
     */
    private List<AdGroup> getAdGroupList() throws FacebookAdApiException {
        try {
            return client.fetchConnection("act_" + adAccount + "/adgroups", AdGroup.class,
                    Parameter.with("limit", 1000), Parameter.with("offset", 0), Parameter.with("fields", "id,name,campaign_id")).getData();
        } catch (final FacebookException e) {
            throw new FacebookAdApiException("Error on Facebook API access ("
                    + e.getClass().getCanonicalName() + ")", e);
        }
    }

    @Override
    public Map<String, FacebookAdsStatsData> getAdGroupStats(final Date startDateSrc, final Date endDateSrc)
            throws FacebookAdApiException {
        init();
        final Date startDate = getDateWithHourShiftForFacebook(startDateSrc);
        final Date endDate = getDateWithHourShiftForFacebook(endDateSrc);
        final List<AdGroup> ads = getAdGroupList();
        final Map<String, String> idToName = new HashMap<String, String>();
        for (final AdGroup ad : ads) {
            idToName.put(Long.toString(ad.getId()), ad.getName().toUpperCase());
        }
        final List<FacebookAdsStatsData> statsHolder = getAdGroups(startDate, endDate);

        final Map<String, FacebookAdsStatsData> statsMap = new HashMap<String, FacebookAdsStatsData>();
        for (final FacebookAdsStatsData stat : statsHolder) {
            if (stat.getSpent() > 0 || stat.getClicks() > 0) {
                statsMap.put(getAdGroupName(idToName, stat), stat);
            }
        }

        return statsMap;
    }

    // See http://developers.facebook.com/docs/reference/ads-api/adstatistics/
    // Our ad acccount is in CST (Europe/Paris) and servers are in UTC
    protected Date getDateWithHourShiftForFacebook(Date sourceDate) {
        return new DateTime(sourceDate).plusHours(timeShift).toDate();
    }

    private String getAdGroupName(final Map<String, String> idToName, final FacebookAdsStatsData stat) {
        String adGroupName = idToName.get(stat.getId());
        if (adGroupName == null) {
            adGroupName = stat.getId();
        }
        return adGroupName;
    }

    /**
     * Gets the statistics for the given list of ads
     *
     * @param startDate Start of the date range we wish to work with
     * @param endDate   End of the date range we wish to work with
     * @return Object holding the statistics of the listed campaigns
     */
    private List<FacebookAdsStatsData> getAdGroups(final Date startDate, final Date endDate) {

        long startDateInSeconds = startDate.getTime() / DateTimeConstants.MILLIS_PER_SECOND;
        long endDateInSeconds = endDate.getTime() / DateTimeConstants.MILLIS_PER_SECOND;

        LOG.debug("Facebook AdGroupStats Call startDate:{}, in seconds:{} and endDate: {} , in seconds:{}",
                startDate, startDateInSeconds, endDate, endDateInSeconds);

        return client.fetchConnection("act_" + adAccount + "/adgroupstats", FacebookAdsStatsData.class,
                Parameter.with("include_deleted", true), Parameter.with("limit", -1),
                Parameter.with("start_time", startDateInSeconds),
                Parameter.with("end_time", endDateInSeconds)).getData();
    }

}
