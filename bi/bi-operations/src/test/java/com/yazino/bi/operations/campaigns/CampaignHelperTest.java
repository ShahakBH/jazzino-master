package com.yazino.bi.operations.campaigns;

import com.google.common.collect.ImmutableMap;
import com.yazino.bi.operations.campaigns.controller.Campaign;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.domain.MessageContentType;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.promotions.BuyChipsForm;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.yazino.engagement.ChannelType.*;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.*;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;

public class CampaignHelperTest {

    private static final DateTime CURRENT_DATE_TIME = new DateTime();
    private Map<String, String> supportedGameTypes = newHashMap();

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(CURRENT_DATE_TIME.getMillis());
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }


    @Test
    public void getSupportedPaymentMethodsShouldReturnExpectedPaymentMethods() throws Exception {
        List<PaymentPreferences.PaymentMethod> supportedPaymentMethods = CampaignHelper.getSupportedPaymentMethods(asList(Platform.values()));

        assertThat(supportedPaymentMethods, hasItems(CREDITCARD, PAYPAL, ITUNES, GOOGLE_CHECKOUT, FACEBOOK, AMAZON));
    }


    @Test
    public void getSupportedPaymentMethodsShouldReturnCreditCardAndPaypalForWebPlatform() throws Exception {
        List<PaymentPreferences.PaymentMethod> supportedPaymentMethods = CampaignHelper.getSupportedPaymentMethods(asList(Platform.WEB));

        assertThat(supportedPaymentMethods, contains(CREDITCARD, PAYPAL));
    }

    @Test
    public void getSupportedPaymentMethodsShouldNotReturnZongOrTrialPay() throws Exception {
        List<PaymentPreferences.PaymentMethod> supportedPaymentMethods = CampaignHelper.getSupportedPaymentMethods(asList(Platform.values()));

        assertThat(supportedPaymentMethods, not(hasItem(ZONG)));
        assertThat(supportedPaymentMethods, not(hasItem(TRIALPAY)));
    }

    @Test
    public void getAllAvailableChipPercentagesShouldReturnAvailablePercentages() {

        Map<BigDecimal, BigDecimal> allAvailablePercentages = CampaignHelper.getAllAvailableChipPercentages();

        Map<BigDecimal, BigDecimal> expectedAvailablePercentages = new LinkedHashMap<>();
        expectedAvailablePercentages.put(BigDecimal.ZERO, BigDecimal.ZERO);
        expectedAvailablePercentages.put(new BigDecimal("50"), new BigDecimal("50"));
        expectedAvailablePercentages.put(new BigDecimal("100"), new BigDecimal("100"));
        expectedAvailablePercentages.put(new BigDecimal("150"), new BigDecimal("150"));
        expectedAvailablePercentages.put(new BigDecimal("200"), new BigDecimal("200"));

        assertThat(allAvailablePercentages, is(expectedAvailablePercentages));
    }

    @Test
    public void getDefaultCampaignForViewShouldReturnCampaignWithContentAndChannelsSet() {
        Campaign defaultCampaignForView = CampaignHelper.getDefaultCampaignForView(supportedGameTypes);
        assertThat(defaultCampaignForView, is(getDefaultCampaign()));
    }

    private Campaign getDefaultCampaign() {
        Map<String, String> contentMap = newLinkedHashMap();
        contentMap.put(MessageContentType.DESCRIPTION.getKey(), "Yazino description");
        contentMap.put(MessageContentType.MESSAGE.getKey(), "Hooray !! Time to have fun");
        contentMap.put(MessageContentType.TRACKING.getKey(), "BI_DEFAULT");

        List<ChannelType> channels = newArrayList(
                FACEBOOK_APP_TO_USER_REQUEST,
                IOS,
                GOOGLE_CLOUD_MESSAGING_FOR_ANDROID,
                AMAZON_DEVICE_MESSAGING);

        String sqlQuery = "SELECT 1 AS PLAYER_ID";

        final Map<NotificationChannelConfigType, String> config = of(
                NotificationChannelConfigType.GAME_TYPE_FILTER, "",
                NotificationChannelConfigType.FILTER_OUT_120_DAY_UNOPENED, "FALSE",
                NotificationChannelConfigType.TEMPLATE, "");

        return new Campaign(0L,
                "Enter Campaign Name",
                new DateTime(),
                new DateTime().plusDays(1),
                168L,
                0l, sqlQuery,
                contentMap,
                channels,
                Boolean.FALSE,
                config,
                false);
    }

    @Test
    public void getDefaultBuyChipsFormShouldReturnBuyChipsFormWithChipsPackagePercentagesSet() {

        BuyChipsForm defaultBuyChipsForm = CampaignHelper.getDefaultBuyChipsForm();

        BuyChipsForm buyChipsForm = new BuyChipsForm();
        Map<Integer, BigDecimal> chipsPackagePercentages = new LinkedHashMap<>();
        for (int packageIndex = 1; packageIndex <= 6; packageIndex++) {
            chipsPackagePercentages.put(packageIndex, BigDecimal.ZERO);
        }
        buyChipsForm.setChipsPackagePercentages(chipsPackagePercentages);

        assertThat(defaultBuyChipsForm, is(buyChipsForm));

    }
}
