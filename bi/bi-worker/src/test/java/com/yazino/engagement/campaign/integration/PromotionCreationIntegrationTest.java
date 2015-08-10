package com.yazino.engagement.campaign.integration;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.promotions.BuyChipsForm;
import com.yazino.promotions.PromotionConfigKeyEnum;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import strata.server.lobby.api.promotion.ControlGroupFunctionType;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionConfiguration;
import strata.server.lobby.api.promotion.PromotionType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.yazino.promotions.PromotionConfigKeyEnum.*;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PromotionCreationIntegrationTest {
    public static final List<Platform> PLATFORMS = asList(Platform.ANDROID, Platform.IOS, Platform.WEB);
    private SystemUnderTest sut;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2013, 6, 28, 11, 0).getMillis());
        sut = new SystemUnderTest();
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void campaignRunsShouldCreatePromotionsWithCorrectValuesIfTheyHaveOne() throws SQLException {
        sut.createPlayers("player1", "player2");
        sut.playedYesterday("player1");
        final Long campaignId = sut.createCampaign("progressive_bonus", "this is a message", new DateTime(), Boolean.TRUE);

        BuyChipsForm buyChipsForm = new BuyChipsForm();
        buyChipsForm.setPromotionDefinitionId(null);
        buyChipsForm.setName("Promo Name");
        buyChipsForm.setInGameNotificationHeader("in Game header");
        buyChipsForm.setInGameNotificationMsg("In game Msg");
        buyChipsForm.setRolloverHeader("roll over header");
        buyChipsForm.setRolloverText("roll over text");
        buyChipsForm.setMaxRewards(1);
        buyChipsForm.setValidForHours(128);
        buyChipsForm.setPriority(1);
        buyChipsForm.setPlatforms(PLATFORMS);
        buyChipsForm.setCampaignId(campaignId);


        sut.createBuyChipsPromotionWith(buyChipsForm);

        sut.campaignServiceRunsCampaign(campaignId, new DateTime());

        assertThat(sut.hasCreatedPromotion(), is(Boolean.TRUE));
        final Promotion lastCreatedPromotion = sut.getLastCreatedPromotion();


        assertThat(lastCreatedPromotion.getName(), equalTo(buyChipsForm.getName()));
        assertThat(lastCreatedPromotion.getPromotionType(), equalTo(PromotionType.BUY_CHIPS));
        assertThat(lastCreatedPromotion.getPriority(), equalTo(buyChipsForm.getPriority()));
        assertThat(lastCreatedPromotion.getStartDate(), equalTo(new DateTime()));
        assertThat(lastCreatedPromotion.getEndDate(), equalTo(new DateTime().plusHours(buyChipsForm.getValidForHours())));
        assertThat(lastCreatedPromotion.getPlatforms(), equalTo(buyChipsForm.getPlatforms()));

        final PromotionConfiguration configuration = lastCreatedPromotion.getConfiguration();
        assertThat(configuration.getConfigurationValue(ROLLOVER_HEADER_KEY.getDescription()), equalTo(buyChipsForm.getRolloverHeader()));
        assertThat(configuration.getConfigurationValue(ROLLOVER_TEXT_KEY.getDescription()), equalTo(buyChipsForm.getRolloverText()));
        assertThat(configuration.getConfigurationValue(IN_GAME_NOTIFICATION_HEADER_KEY.getDescription()),
                equalTo(buyChipsForm.getInGameNotificationHeader()));
        assertThat(configuration.getConfigurationValue(IN_GAME_NOTIFICATION_MSG_KEY.getDescription()),
                equalTo(buyChipsForm.getInGameNotificationMsg()));

        assertThat(lastCreatedPromotion.getControlGroupFunction(), equalTo(ControlGroupFunctionType.PLAYER_ID));
        assertThat(lastCreatedPromotion.getControlGroupPercentage(), equalTo(0));

        String paymentMethodsAsString = configuration.getConfigurationValue(PromotionConfigKeyEnum.PAYMENT_METHODS.getDescription());
        assertThat(getPaymentMethodsListFromString(paymentMethodsAsString), Matchers.hasItems(
                PaymentPreferences.PaymentMethod.CREDITCARD,
                PaymentPreferences.PaymentMethod.PAYPAL,
                PaymentPreferences.PaymentMethod.ITUNES,
                PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT));

    }

    private List<PaymentPreferences.PaymentMethod> getPaymentMethodsListFromString(final String paymentMethodsString) throws SQLException {
        final String[] paymentStringArray = paymentMethodsString.split(",");
        final List<PaymentPreferences.PaymentMethod> paymentMethodList = new ArrayList<PaymentPreferences.PaymentMethod>();

        for (String paymentMethod : paymentStringArray) {
            paymentMethodList.add(PaymentPreferences.PaymentMethod.valueOf(paymentMethod.toUpperCase()));
        }

        return paymentMethodList;
    }


}
