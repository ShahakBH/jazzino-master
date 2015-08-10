package com.yazino.bi.operations.campaigns;

import com.yazino.bi.operations.campaigns.controller.Campaign;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.domain.MessageContentType;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.promotions.BuyChipsForm;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.*;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.join;

public final class CampaignHelper {

    private static Map<Platform, List<PaymentPreferences.PaymentMethod>> supportedPayments = new HashMap<>();

    static {
        supportedPayments.put(Platform.WEB, asList(CREDITCARD, PAYPAL));
        supportedPayments.put(Platform.IOS, asList(ITUNES));
        supportedPayments.put(Platform.FACEBOOK_CANVAS, asList(FACEBOOK));
        supportedPayments.put(Platform.ANDROID, asList(GOOGLE_CHECKOUT));
        supportedPayments.put(Platform.AMAZON, asList(AMAZON));
    }

    private CampaignHelper() {
    }

    public static List<PaymentPreferences.PaymentMethod> getSupportedPaymentMethods(List<Platform> platforms) {

        List<PaymentPreferences.PaymentMethod> allAvailablePaymentMethods = newArrayList();

        for (Platform platform : platforms) {
            List<PaymentPreferences.PaymentMethod> paymentMethodsForPlatform = supportedPayments.get(platform);
            if (paymentMethodsForPlatform != null) {
                allAvailablePaymentMethods.addAll(paymentMethodsForPlatform);
            }
        }

        return allAvailablePaymentMethods;
    }


    public static Map<BigDecimal, BigDecimal> getAllAvailableChipPercentages() {

        //TODO retrieve this value from the db so that we can change them dynamically
        Map<BigDecimal, BigDecimal> availablePercentages = new LinkedHashMap<>();
        availablePercentages.put(BigDecimal.ZERO, BigDecimal.ZERO);
        availablePercentages.put(new BigDecimal("50"), new BigDecimal("50"));
        availablePercentages.put(new BigDecimal("100"), new BigDecimal("100"));
        availablePercentages.put(new BigDecimal("150"), new BigDecimal("150"));
        availablePercentages.put(new BigDecimal("200"), new BigDecimal("200"));
        return availablePercentages;
    }

    public static List<ChannelType> getSupportedChannelList() {
        List<ChannelType> channels = newArrayList(ChannelType.values());
        return channels;
    }

    public static Campaign getDefaultCampaignForView(final Map<String, String> supportedGameTypes) {

        Map<String, String> contentMap = newLinkedHashMap();
        final Map<NotificationChannelConfigType, String> channelConfig = newHashMap();
        channelConfig.put(NotificationChannelConfigType.TEMPLATE, "");
        channelConfig.put(NotificationChannelConfigType.FILTER_OUT_120_DAY_UNOPENED, "FALSE");
        channelConfig.put(NotificationChannelConfigType.GAME_TYPE_FILTER, join(supportedGameTypes.keySet(), ","));

        contentMap.put(MessageContentType.DESCRIPTION.getKey(), "Yazino description");
        contentMap.put(MessageContentType.MESSAGE.getKey(), "Hooray !! Time to have fun");
        contentMap.put(MessageContentType.TRACKING.getKey(), "BI_DEFAULT");

        List<ChannelType> channels = getSupportedChannelList();
        channels.remove(ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION);
        channels.remove(ChannelType.EMAIL);

        String sqlQuery = "SELECT 1 AS PLAYER_ID";

        String campaignName = "Enter Campaign Name";
        long runHours = 168L;
        long runMinutes = 0L;
        return new Campaign(0L,
                campaignName,
                new DateTime(),
                new DateTime().plusDays(1),
                runHours,
                runMinutes, sqlQuery,
                contentMap,
                channels,
                Boolean.FALSE,
                channelConfig,
                false
        );
    }

    public static BuyChipsForm getDefaultBuyChipsForm() {
        BuyChipsForm buyChipsForm = new BuyChipsForm();
        Map<Integer, BigDecimal> chipsPackagePercentages = new LinkedHashMap<>();
        for (int packageIndex = 1; packageIndex <= 6; packageIndex++) {
            chipsPackagePercentages.put(packageIndex, BigDecimal.ZERO);
        }
        buyChipsForm.setChipsPackagePercentages(chipsPackagePercentages);
        return buyChipsForm;
    }
}
