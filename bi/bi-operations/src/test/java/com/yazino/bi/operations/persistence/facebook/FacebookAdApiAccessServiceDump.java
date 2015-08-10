package com.yazino.bi.operations.persistence.facebook;

import com.restfb.DefaultFacebookClient;
import com.restfb.DefaultWebRequestor;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.json.JsonObject;
import org.joda.time.DateTime;
import org.junit.Test;
import com.yazino.bi.operations.persistence.facebook.data.AdGroup;
import com.yazino.bi.operations.persistence.facebook.data.Campaign;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;
import com.yazino.bi.operations.persistence.facebook.data.TimeRange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * In order to get a new code if this one doesn't work, we need:
 * <p/>
 * - To request the token by doing:
 * https://www.facebook.com/dialog/oauth?client_id=184292374955779&redirect_uri=http://apps.facebook.com/yazino_ads/&response_type=token
 * - Then intercept the response before the redirect that should be like:
 * https://apps.facebook.com/yazino_ads/#access_token=184292374955779%7C743bb06fe3f61520ececf675.1-598695404%7CFBZe_XnzHjJOSKl3HZtm-iTYTAM&expires_in=0
 * https://apps.facebook.com/yazino_ads/#access_token=AAACnnOmLRwMBAOkFZANA3poiBjh8H7qJhi9EoSrDmNVvSbxC0LZAG02ScF5ogyJMui3F3iXLOlgpZC9R7VZCKzHq6pWDm8QZD&expires_in=0
 */
public class FacebookAdApiAccessServiceDump {

    private static final String FACEBOOK_ACCESS_TOKEN_THAT_NEEDS_REPLACING_EVERY_FEW_MONTHS = "AAACnnOmLRwMBAJgKIdsV43YCjWdv1at3c63VtreQwfpUVfiZA2t1dYA8LCRBgKOsYupNlXUcIl3kc1oMjGgPpFey8QJaMOGbVaDPR5QZDZD";
    //this needs replacing every few months and you can get it from the env.props in bi-operations

    @Test
    public void shouldAccessFacebookGraphApi() {
        final FacebookClient client =
                new DefaultFacebookClient(
                        FACEBOOK_ACCESS_TOKEN_THAT_NEEDS_REPLACING_EVERY_FEW_MONTHS,
                        // "184292374955779|013d2af86dbc691d6474324c.1-598695404|1keZysW3gbGaI_210NYqUrykc8k",
                        new DefaultWebRequestor(), new MapsCapableJsonMapper());

        client.fetchObject("me/adaccounts", JsonObject.class);
        final List<Campaign> campaigns =
                client.fetchConnection("act_354857149/adcampaigns", Campaign.class,
                        Parameter.with("limit", 2), Parameter.with("offset", 0),
                        Parameter.with("include_count", false)).getData();

        System.out.println("" + campaigns.size() + "/" + campaigns.get(0).getAccountId() + "/"
                + campaigns.get(0).getCampaignId() + "/" + campaigns.get(0).getName());

        List<String> selectList = new ArrayList<String>();
        selectList.add(campaigns.get(0).getCampaignId());
        selectList.add(campaigns.get(1).getCampaignId());

        final List<TimeRange> timesList = new ArrayList<TimeRange>();
        timesList.add(new TimeRange(2011, 1, 1, 2011, 8, 1));

        List<FacebookAdsStatsData> stats =
                client.fetchConnection(
                        "act_354857149/adcampaignstats",
                        FacebookAdsStatsData.class,
                        Parameter.with("campaign_ids", selectList),
                        Parameter.with("include_deleted", true),
                        Parameter.with("start_time",
                                new DateTime(2011, 1, 1, 0, 0, 0, 0).toDate().getTime() / 1000L),
                        Parameter.with("end_time",
                                new DateTime(2011, 8, 1, 0, 0, 0, 0).toDate().getTime() / 1000L)).getData();
        System.out.println("" + stats.size() + " @ " + stats.get(0).getImpressions());

        final List<AdGroup> groups =
                client.fetchConnection("act_354857149/adgroups", AdGroup.class, Parameter.with("limit", 2),
                        Parameter.with("offset", 0), Parameter.with("include_count", false)).getData();
        System.out.println(groups.size() + "/" + groups.get(0).getId() + "-" + groups.get(0).getId()
                + "/" + groups.get(0).getName() + " / " + groups.get(1).getId() + "-"
                + groups.get(1).getId() + "/" + groups.get(1).getName());

        selectList = new ArrayList<String>();
        selectList.add(Long.toString(groups.get(0).getId()));
        selectList.add(Long.toString(groups.get(1).getId()));

        stats =
                client.fetchConnection(
                        "act_354857149/adgroupstats",
                        FacebookAdsStatsData.class,
                        Parameter.with("account_id", "354857149"),
                        Parameter.with("adgroup_ids", selectList),
                        Parameter.with("include_deleted", true),
                        Parameter.with("start_time",
                                new DateTime(2011, 1, 1, 0, 0, 0, 0).toDate().getTime() / 1000L),
                        Parameter.with("end_time",
                                new DateTime(2011, 8, 1, 0, 0, 0, 0).toDate().getTime() / 1000L)).getData();
        System.out.println("" + stats.size());
    }

    private List<PermBoxInsightsHolder> getInsights(final FacebookClient client, final String appId) {
        return client.fetchConnection(appId + "/insights/application_permission_grants_top",
                PermBoxInsightsHolder.class,
                // Parameter.with("period", "day"),
                Parameter.with("since", "2011-11-01"), Parameter.with("until", "2011-12-01")).getData();
    }

    private List<InstallationInsightsHolder> getInstallationInsights(final FacebookClient client,
                                                                     final String appId) {
        return client.fetchConnection(appId + "/insights/application_installation_adds_unique",
                InstallationInsightsHolder.class,
                // Parameter.with("period", "day"),
                Parameter.with("since", "2011-11-01"), Parameter.with("until", "2011-12-01")).getData();
    }

    private void addStats(final Map<String, PermBoxInsightsStats> stats, final PermBoxInsights insights) {
        if (insights.getStats().getMailImpressions() == null) {
            return;
        }
        final String date = insights.getEndTime().substring(0, 10);
        PermBoxInsightsStats actualHolder = stats.get(date);
        if (actualHolder == null) {
            actualHolder = new PermBoxInsightsStats();
            actualHolder.setMailImpressions(0L);
            actualHolder.setInstallations(0L);
            stats.put(date, actualHolder);
        }
        actualHolder.setMailImpressions(actualHolder.getMailImpressions()
                + insights.getStats().getMailImpressions());
    }

    private void addInstallationStats(final Map<String, PermBoxInsightsStats> stats,
                                      final InstallationInsights insights) {
        if (insights.getValue() == null) {
            return;
        }
        final String date = insights.getEndTime().substring(0, 10);
        PermBoxInsightsStats actualHolder = stats.get(date);
        if (actualHolder == null) {
            actualHolder = new PermBoxInsightsStats();
            actualHolder.setMailImpressions(0L);
            actualHolder.setInstallations(0L);
            stats.put(date, actualHolder);
        }
        actualHolder.setInstallations(actualHolder.getInstallations() + insights.getValue());
    }

    @Test
    public void shouldGetInsights() {
        final FacebookClient client =
                new DefaultFacebookClient(
                        FACEBOOK_ACCESS_TOKEN_THAT_NEEDS_REPLACING_EVERY_FEW_MONTHS,
                        new DefaultWebRequestor(), new MapsCapableJsonMapper());
        final Map<String, PermBoxInsightsStats> stats = new LinkedHashMap<String, PermBoxInsightsStats>();

        System.out.println("Blackjack");
        List<PermBoxInsightsHolder> holder = getInsights(client, "111506931275");
        if (!holder.isEmpty()) {
            System.out.println("" + holder.get(0).getId());
            for (final PermBoxInsights insights : holder.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getStats().getMailImpressions());
                addStats(stats, insights);
            }
        }
        System.out.println("Installations");
        List<InstallationInsightsHolder> installationInsights =
                getInstallationInsights(client, "111506931275");
        if (!installationInsights.isEmpty()) {
            for (final InstallationInsights insights : installationInsights.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getValue());
                addInstallationStats(stats, insights);
            }
        }

        System.out.println("Slots");
        holder = getInsights(client, "90279114378");
        if (!holder.isEmpty()) {
            System.out.println("" + holder.get(0).getId());
            for (final PermBoxInsights insights : holder.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getStats().getMailImpressions());
                addStats(stats, insights);
            }
        }
        System.out.println("Installations");
        installationInsights = getInstallationInsights(client, "90279114378");
        if (!installationInsights.isEmpty()) {
            for (final InstallationInsights insights : installationInsights.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getValue());
                addInstallationStats(stats, insights);
            }
        }

        System.out.println("Roulette");
        holder = getInsights(client, "102380207296");
        if (!holder.isEmpty()) {
            System.out.println("" + holder.get(0).getId());
            for (final PermBoxInsights insights : holder.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getStats().getMailImpressions());
                addStats(stats, insights);
            }
        }
        System.out.println("Installations");
        installationInsights = getInstallationInsights(client, "102380207296");
        if (!installationInsights.isEmpty()) {
            for (final InstallationInsights insights : installationInsights.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getValue());
                addInstallationStats(stats, insights);
            }
        }

        System.out.println("Hissteria");
        holder = getInsights(client, "204441229583839");
        if (!holder.isEmpty()) {
            System.out.println("" + holder.get(0).getId());
            for (final PermBoxInsights insights : holder.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getStats().getMailImpressions());
                addStats(stats, insights);
            }
            System.out.println("Installations");
            installationInsights = getInstallationInsights(client, "204441229583839");
            for (final InstallationInsights insights : installationInsights.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getValue());
                addInstallationStats(stats, insights);
            }
        }


        System.out.println("XT Bingo");
        holder = getInsights(client, "230301260322307");
        if (!holder.isEmpty()) {
            System.out.println("" + holder.get(0).getId());
            for (final PermBoxInsights insights : holder.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getStats().getMailImpressions());
                addStats(stats, insights);
            }
            System.out.println("Installations");
            installationInsights = getInstallationInsights(client, "230301260322307");
            for (final InstallationInsights insights : installationInsights.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getValue());
                addInstallationStats(stats, insights);
            }
        }

        if (!holder.isEmpty()) {
            System.out.println("High stakes");
            holder = getInsights(client, "233329776698495");
            System.out.println("" + holder.get(0).getId());
            for (final PermBoxInsights insights : holder.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getStats().getMailImpressions());
                addStats(stats, insights);
            }
            System.out.println("Installations");
            installationInsights = getInstallationInsights(client, "233329776698495");
            for (final InstallationInsights insights : installationInsights.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getValue());
                addInstallationStats(stats, insights);
            }
        }

        System.out.println("Poker");
        holder = getInsights(client, "89403541167");
        if (!holder.isEmpty()) {

            System.out.println("" + holder.get(0).getId());
            for (final PermBoxInsights insights : holder.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getStats().getMailImpressions());
                addStats(stats, insights);
            }
            System.out.println("Installations");
            installationInsights = getInstallationInsights(client, "89403541167");
            for (final InstallationInsights insights : installationInsights.get(0).getValues()) {
                System.out.println("" + insights.getEndTime() + " @ " + insights.getValue());
                addInstallationStats(stats, insights);
            }

            System.out.println("Totals");
            for (final Entry<String, PermBoxInsightsStats> entry : stats.entrySet()) {
                System.out.println(entry.getKey() + "\t" + entry.getValue().getMailImpressions() + "\t"
                        + entry.getValue().getInstallations());
            }
        }
    }
}
