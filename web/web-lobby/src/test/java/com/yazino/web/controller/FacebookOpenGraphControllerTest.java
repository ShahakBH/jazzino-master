package com.yazino.web.controller;

import com.yazino.web.domain.facebook.FacebookOGResource;
import com.yazino.web.domain.facebook.FacebookOGResources;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class FacebookOpenGraphControllerTest {

    private static final String APP_ID = "appId";
    private static final String HAY_STEKS = "hay_steks";
    private static final String OBJECT_ID = "HIGH_STAKES_earnchips";
    private FacebookOpenGraphController underTest;
    private Map<String, FacebookOGResource> resourceMap = new HashMap<String, FacebookOGResource>();
    private HttpServletResponse response;
    private FacebookConfiguration facebookConfiguration;
    private FacebookAppConfiguration facebookAppConfiguration;
    private static final String BLACKJACK_PREFIX = "bj";
    private static final String SLOTS_PREFIX = "wd";
    private FacebookOGResource facebookOGResource;
    private FacebookOGResource sampleLevel;

    @Before
    public void setUp() throws Exception {
        response = mock(HttpServletResponse.class);

        facebookOGResource = new FacebookOGResource();
        facebookOGResource.setArticle("a");
        facebookOGResource.setDescription("Join the world’s first social slots game and play LIVE against millions of players.");
        facebookOGResource.setTitle("title for abc123");

        sampleLevel = new FacebookOGResource();
        sampleLevel.setArticle("");
        sampleLevel.setDescription("sampleDescription");
        sampleLevel.setTitle("sampleTitle");

        resourceMap.put("wd_abc123",facebookOGResource );
        resourceMap.put("wd_level_10", sampleLevel);
        facebookConfiguration = mock(FacebookConfiguration.class);
        underTest = new FacebookOpenGraphController(new FacebookOGResources(resourceMap), facebookConfiguration);
        facebookAppConfiguration = mock(FacebookAppConfiguration.class);
    }

    @Test
    public void requestShouldReturnViewForBigWin() throws IOException {

        when(facebookConfiguration.getAppConfigForOpenGraphObjectPrefix(SLOTS_PREFIX)).thenReturn(facebookAppConfiguration);
        when(facebookAppConfiguration.getApplicationId()).thenReturn("slots123");
        when(facebookAppConfiguration.getAppName()).thenReturn("slots");


        final ModelAndView ogDataModel = underTest.getOGData("wd_abc123", "objectType", "someAdTrackingCode",
                response);
        assertThat(ogDataModel.getViewName(), is(equalTo("fbog/object")));
        assertThat((String) ogDataModel.getModel().get("objectTitle"), is(equalTo("title for abc123")));
        assertThat((String) ogDataModel.getModel().get("objectId"), is(equalTo("wd_abc123")));
        assertThat((String) ogDataModel.getModel().get("objectType"), is(equalTo("objectType")));
        assertThat((String) ogDataModel.getModel().get("appId"), is(equalTo("slots123")));
        assertThat((String) ogDataModel.getModel().get("appName"), is(equalTo("slots")));
        assertThat((String) ogDataModel.getModel().get("objectDesc"), is(equalTo("Join the world’s first social slots game and play LIVE against millions of players.")));
        assertThat((String) ogDataModel.getModel().get("ref"), is(equalTo("someAdTrackingCode")));
    }

    @Test
    public void requestShouldFailForUnknownObject() throws IOException {
        final ModelAndView ogDataModel = underTest.getOGData("unknown", "objectType", "someAdTrackingCode", response);
        assertNull(ogDataModel);
    }

    @Test
    public void requestShouldFailWithInvalidLevelId() throws IOException {
        final ModelAndView ogDataModel = underTest.handleLevel("invalid_id", "someAdTrackingCode", response);
        assertNull(ogDataModel);
    }

    @Test
    public void handleLevel_requestShouldFailWithUnrecognizedOGPrefix() throws IOException {

        when(facebookConfiguration.getAppConfigForOpenGraphObjectPrefix(SLOTS_PREFIX)).thenReturn(null);

        underTest.handleLevel("ee_id_1", "someAdTrackingCode", response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid id passed to FB OG Controller:ee_id_1");
    }


    @Test
    public void requestShouldReturnViewForNewLevel() throws IOException {

        when(facebookConfiguration.getAppConfigForOpenGraphObjectPrefix(SLOTS_PREFIX)).thenReturn(facebookAppConfiguration);
        when(facebookAppConfiguration.getApplicationId()).thenReturn("slots123");
        when(facebookAppConfiguration.getAppName()).thenReturn("slots");

        final ModelAndView ogDataModel = underTest.handleLevel("wd_level_10", "someAdTrackingCode",
                response);
        assertThat(ogDataModel.getViewName(), is(equalTo("fbog/object")));
        assertThat((String) ogDataModel.getModel().get("objectTitle"), is(equalTo("sampleTitle")));
        assertThat((String) ogDataModel.getModel().get("objectDesc"), is(equalTo("sampleDescription")));
        assertThat((String) ogDataModel.getModel().get("objectId"), is(equalTo("wd_level_10")));
        assertThat((String) ogDataModel.getModel().get("objectImg"), is(equalTo("wd_level_10")));
        assertThat((String) ogDataModel.getModel().get("objectType"), is(equalTo("level")));
        assertThat((String) ogDataModel.getModel().get("appId"), is(equalTo("slots123")));
        assertThat((String) ogDataModel.getModel().get("appName"), is(equalTo("slots")));
        assertThat((String) ogDataModel.getModel().get("ref"), is(equalTo("someAdTrackingCode")));
    }

    // check loads level by number
    // check title/description/image
    // check cycling of images

    @Test
    public void getCurrencyInfoShouldReturnOpengraphCurrencyForCorrectGameType() throws IOException {
        when(facebookConfiguration.getAppConfigFor("HIGH_STAKES", FacebookConfiguration.ApplicationType.CANVAS,
                FacebookConfiguration.MatchType.STRICT)).thenReturn(facebookAppConfiguration);
        when(facebookAppConfiguration.getApplicationId()).thenReturn(APP_ID);
        when(facebookAppConfiguration.getAppName()).thenReturn(HAY_STEKS);

        final ModelAndView mav = underTest.getCurrencyInfo("HIGH_STAKES_earnchips", response);

        assertThat(mav.getViewName(), is(equalTo("fbog/currency")));
        assertThat((String)mav.getModel().get("appId"), is(equalTo(APP_ID)));
        assertThat((String)mav.getModel().get("appName"), is(equalTo(HAY_STEKS)));
        assertThat((String)mav.getModel().get("objectType"), is(equalTo("currency")));
        assertThat((String)mav.getModel().get("objectId"), is(equalTo(OBJECT_ID)));

    }

    @Test
    public void getProductIdForFacebookPackageShouldShowPackageDetails() throws IOException {
        final ModelAndView ogDataModel = underTest.getProductInfo("gbp1_3_buys_20k", response);
        assertThat(ogDataModel.getViewName(), is(equalTo("fbog/product")));
        assertThat((String) ogDataModel.getModel().get("objectId"), is(equalTo("gbp1_3_buys_20k")));
        assertThat((String) ogDataModel.getModel().get("objectImg"), is(equalTo("1")));
        assertThat((String) ogDataModel.getModel().get("objectTitle"), is(equalTo("20000 Chips")));
        assertThat((String) ogDataModel.getModel().get("objectDesc"), is(equalTo("20000 Chips")));
    }

    @Test
    public void getProductIdForAUDFacebookPackageShouldShowPackageDetails() throws IOException {
        final ModelAndView ogDataModel = underTest.getProductInfo("aud3_19.40_buys_50k", response);
        assertThat(ogDataModel.getViewName(), is(equalTo("fbog/product")));
        assertThat((String) ogDataModel.getModel().get("objectId"), is(equalTo("aud3_19.40_buys_50k")));
        assertThat((String) ogDataModel.getModel().get("objectImg"), is(equalTo("3")));
        assertThat((String) ogDataModel.getModel().get("objectTitle"), is(equalTo("50000 Chips")));
        assertThat((String) ogDataModel.getModel().get("objectDesc"), is(equalTo("50000 Chips")));
    }

    @Test
    public void getProductIdForFacebookPackageWithPromoShouldShowPackageDetails() throws IOException {
        final ModelAndView ogDataModel = underTest.getProductInfo("gbp1_3_buys_20k_8", response);
        assertThat(ogDataModel.getViewName(), is(equalTo("fbog/product")));
        assertThat((String) ogDataModel.getModel().get("objectId"), is(equalTo("gbp1_3_buys_20k_8")));
        assertThat((String) ogDataModel.getModel().get("objectImg"), is(equalTo("1")));
        assertThat((String) ogDataModel.getModel().get("objectTitle"), is(equalTo("20000 Chips")));
        assertThat((String) ogDataModel.getModel().get("objectDesc"), is(equalTo("20000 Chips")));
    }
}
