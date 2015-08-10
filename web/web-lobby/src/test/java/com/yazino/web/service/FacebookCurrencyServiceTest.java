package com.yazino.web.service;

import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.json.JsonObject;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.reference.ReferenceService;
import com.yazino.web.data.CurrencyRepository;
import com.yazino.web.domain.facebook.FacebookClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FacebookCurrencyServiceTest {
    private static final String UK_IP_ADDRESS = "88.211.55.18";
    private static final String NORWAY_IP_ADDRESS = "213.52.50.8";

    public static final String GAME_TYPE = "SLOTS";
    public static final String AN_ACCESS_TOKEN = "an access token";
    @Mock
    private FacebookClientFactory facebookClientFactory;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private ReferenceService referenceService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession httpSession;

    private FacebookCurrencyService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new FacebookCurrencyService(referenceService, facebookClientFactory, currencyRepository);

        when(request.getSession()).thenReturn(httpSession);
        when(currencyRepository.getAcceptedCurrencies()).thenReturn(newHashSet(Currency.USD, Currency.GBP));
        when(referenceService.getPreferredCurrency("GB")).thenReturn(Currency.GBP);
        when(referenceService.getPreferredCurrency("US")).thenReturn(Currency.USD);
        when(referenceService.getPreferredCurrency("NO")).thenReturn(Currency.NOK);
    }

    @Test
    public void preferredCurrencyShouldDefaultToUSDWhenAccessTokenIsBlankAndCurrencyLookupFromIPIsNotAccepted() {
        when(httpSession.getAttribute("facebookAccessToken." + GAME_TYPE)).thenReturn("");
        when(request.getRemoteAddr()).thenReturn(NORWAY_IP_ADDRESS);

        Currency currency = underTest.getPreferredCurrencyFromFacebook(request, GAME_TYPE);

        assertThat(currency, is(Currency.USD));
    }

    @Test
    public void preferredCurrencyShouldBeGBPWhenAccessTokenIsBlankAndCurrencyLookupFromIPIsGBP() {
        when(httpSession.getAttribute("facebookAccessToken." + GAME_TYPE)).thenReturn("");
        when(request.getRemoteAddr()).thenReturn(UK_IP_ADDRESS);

        Currency currency = underTest.getPreferredCurrencyFromFacebook(request, GAME_TYPE);

        assertThat(currency, is(Currency.GBP));
    }

    @Test
    public void preferredCurrencyShouldDefaultToUSDWhenAccessTokenIsNull() {
        when(httpSession.getAttribute("facebookAccessToken." + GAME_TYPE)).thenReturn(null);

        Currency currency = underTest.getPreferredCurrencyFromFacebook(request, GAME_TYPE);

        assertThat(currency, is(Currency.USD));
    }

    @Test
    public void preferredCurrencyShouldDefaultToUSDWhenAccessTokenIsInvalid() {
        when(httpSession.getAttribute("facebookAccessToken." + GAME_TYPE)).thenReturn(AN_ACCESS_TOKEN);
        when(facebookClientFactory.getClient(anyString())).thenThrow(new com.restfb.exception.FacebookOAuthException(null, null, null, null));

        Currency currency = underTest.getPreferredCurrencyFromFacebook(request, GAME_TYPE);

        assertThat(currency, is(Currency.USD));
    }

    @Test
    public void preferredCurrencyShouldBePlayersFacebookCurrencyWhenAccepted() {
        setUpPlayersFacebookCurrency(Currency.GBP);

        Currency preferredCurrency = underTest.getPreferredCurrencyFromFacebook(request, GAME_TYPE);

        assertThat(preferredCurrency, is(Currency.GBP));
    }

    @Test
    public void preferredCurrencyShouldBeUSDWhenPlayersFacebookCurrencyIsNotAccepted() {
        setUpPlayersFacebookCurrency(Currency.AOA);

        Currency preferredCurrency = underTest.getPreferredCurrencyFromFacebook(request, GAME_TYPE);

        assertThat(preferredCurrency, is(Currency.USD));
    }

    private void setUpPlayersFacebookCurrency(Currency preferredCurrency) {
        when(httpSession.getAttribute("facebookAccessToken." + GAME_TYPE)).thenReturn(AN_ACCESS_TOKEN);
        FacebookClient fbClient = mock(FacebookClient.class);
        when(facebookClientFactory.getClient(AN_ACCESS_TOKEN)).thenReturn(fbClient);
        final JsonObject mockJsonObject = mock(JsonObject.class);
        when(fbClient.fetchObject("me", JsonObject.class, Parameter.with("fields", "currency"))).thenReturn(mockJsonObject);
        final JsonObject currency = new JsonObject("{\n" +
                "      \"user_currency\": \"" + preferredCurrency.getCode() + "\",\n" +
                "      \"currency_exchange\": 4.94258589,\n" +
                "      \"currency_exchange_inverse\": 0.2023232418,\n" +
                "      \"usd_exchange\": 0.494258589,\n" +
                "      \"usd_exchange_inverse\": 2.0232324177,\n" +
                "      \"currency_offset\": 100\n" +
                "   }");
        when(mockJsonObject.getJsonObject("currency")).thenReturn(currency);
    }
}
