package com.yazino.promotions;

import com.yazino.platform.Platform;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.yazino.promotions.BuyChipsForm.PROMO_KEY;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BuyChipsFormTest {

    private Long promotionDefinitionId = 123L;
    private String name = "My promotion";
    private String inGameNotificationMsg = "In game Notification Message";
    private String inGameNotificationHeader = "In game Notification Header";
    private String rolloverHeader = "Roll over Header";
    private String rolloverText = "Roll over Text";
    private Integer maxRewards = 100;
    private Integer validForHours = 10;
    private Integer priority = 12;
    private List<Platform> platforms = asList(Platform.values());
    private Map<Integer, BigDecimal> chipsPackagePercentages = new LinkedHashMap<Integer, BigDecimal>();

    private Long campaignId = 456L;

    @Test
    public void toStringMapFormShouldReturnValuesInMapFormat() {

        chipsPackagePercentages.put(1, BigDecimal.TEN);

        BuyChipsForm buyChipsForm = new BuyChipsForm();
        buyChipsForm.setPromotionDefinitionId(promotionDefinitionId);
        buyChipsForm.setName(name);
        buyChipsForm.setInGameNotificationHeader(inGameNotificationHeader);
        buyChipsForm.setInGameNotificationMsg(inGameNotificationMsg);
        buyChipsForm.setRolloverHeader(rolloverHeader);
        buyChipsForm.setRolloverText(rolloverText);
        buyChipsForm.setMaxRewards(maxRewards);
        buyChipsForm.setValidForHours(validForHours);
        buyChipsForm.setPriority(priority);
        buyChipsForm.setPlatforms(platforms);
        buyChipsForm.setCampaignId(campaignId);
        buyChipsForm.setChipsPackagePercentages(chipsPackagePercentages);

        Map<String, String> buyChipsFormMap = buyChipsForm.toStringMap();

        assertThat(buyChipsFormMap.get(PROMO_KEY), is(""));


        assertThat(buyChipsFormMap.get(PROMO_KEY + "promotionDefinitionId"), is(promotionDefinitionId.toString()));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "name"), is(name));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "inGameNotificationMsg"), is(inGameNotificationMsg));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "inGameNotificationHeader"), is(inGameNotificationHeader));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "rolloverHeader"), is(rolloverHeader));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "rolloverText"), is(rolloverText));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "maxRewards"), is(maxRewards.toString()));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "validForHours"), is(validForHours.toString()));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "priority"), is(priority.toString()));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "platforms"), is(platforms.toString()));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "campaignId"), is(campaignId.toString()));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "chipsPackagePercentages"), is(chipsPackagePercentages.toString()));
    }

    @Test
    public void toStringMapFormShouldReturnValuesInMapFormatForEmptyObject() {

        BuyChipsForm buyChipsForm = new BuyChipsForm();

        Map<String, String> buyChipsFormMap = buyChipsForm.toStringMap();

        String EMPTY_STRING = "";
        assertThat(buyChipsFormMap.get(PROMO_KEY), is(EMPTY_STRING));

        assertThat(buyChipsFormMap.get(PROMO_KEY + "promotionDefinitionId"), is(EMPTY_STRING));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "name"), is(EMPTY_STRING));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "inGameNotificationMsg"), is(EMPTY_STRING));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "inGameNotificationHeader"), is(EMPTY_STRING));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "rolloverHeader"), is(EMPTY_STRING));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "rolloverText"), is(EMPTY_STRING));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "maxRewards"), is("1"));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "validForHours"), is("24"));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "priority"), is("1"));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "platforms"), is(EMPTY_STRING));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "campaignId"), is(EMPTY_STRING));
        assertThat(buyChipsFormMap.get(PROMO_KEY + "chipsPackagePercentages"), is("{}"));
    }

}
