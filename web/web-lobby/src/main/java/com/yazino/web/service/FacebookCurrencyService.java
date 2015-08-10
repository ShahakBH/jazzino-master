package com.yazino.web.service;

import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.json.JsonObject;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.reference.ReferenceService;
import com.yazino.web.data.CurrencyRepository;
import com.yazino.web.domain.facebook.FacebookClientFactory;
import com.yazino.web.domain.facebook.FacebookUserCurrency;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * Service used to get retrieve player's facebook preferred currency.
 */
@Service
public class FacebookCurrencyService {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookCurrencyService.class);
    private FacebookClientFactory facebookClientFactory;
    private CurrencyRepository currencyRepository;
    private ReferenceService referenceService;

    @Autowired
    public FacebookCurrencyService(ReferenceService referenceService,
                                   @Qualifier("facebookClientFactory") FacebookClientFactory facebookClientFactory,
                                   CurrencyRepository currencyRepository) {
        this.referenceService = referenceService;
        this.facebookClientFactory = facebookClientFactory;
        this.currencyRepository = currencyRepository;
    }


    /**
     * Gets the players preferred currency from Facebook. If the currency returned by Facebook is not a Yazino accepted currency
     * then USD is returned. If we fail to get the currency from Facebook for any reason, then we use the default currency players location
     * based on ip address. Again, if this currency is an accepted one then USD is returned.
     * <p/>
     * taken from CashierController
     */
    public Currency getPreferredCurrencyFromFacebook(HttpServletRequest request, String gameType) {
        String preferredCurrencyCode = getPreferredCurrencyCode(request, gameType);
        return getAcceptedCurrencyFromCode(preferredCurrencyCode);
    }

    private String getPreferredCurrencyCode(HttpServletRequest request, String gameType) {
        final String accessToken = (String) request.getSession().getAttribute("facebookAccessToken." + gameType);

        String preferredCurrencyCode;
        if (StringUtils.isBlank(accessToken)) {
            LOG.warn("getting facebook currency failed due to blank accesstoken:%s. defaulting to getting currency from IP", accessToken);
            preferredCurrencyCode = getDefaultCurrencyFromIP(request).getCode();
        } else {
            try {
                FacebookClient client = facebookClientFactory.getClient(accessToken);
                JsonObject currencyObject = client.fetchObject("me", JsonObject.class, Parameter.with("fields", "currency")).getJsonObject("currency");
                preferredCurrencyCode = new FacebookUserCurrency(currencyObject).getUserCurrency();
            } catch (Exception e) {
                LOG.warn("getting facebook currency failed due to invalid accesstoken: %s. defaulting to USD", accessToken);
                preferredCurrencyCode = getDefaultCurrencyFromIP(request).getCode();
            }
        }
        return preferredCurrencyCode;
    }

    private Currency getAcceptedCurrencyFromCode(String currencyCode) {
        for (Currency acceptedCurrency : currencyRepository.getAcceptedCurrencies()) {
            if (acceptedCurrency.getCode().equals(currencyCode)) {
                return acceptedCurrency;
            }
        }
        return Currency.USD;
    }

    private Currency getDefaultCurrencyFromIP(final HttpServletRequest request) {
        LOG.debug("Got request from IP {}", request.getRemoteAddr());
        final String countryCode = new GeolocationLookup().lookupCountryCodeByIp(request.getRemoteAddr());
        LOG.debug("Got request from country {}", countryCode);
        return referenceService.getPreferredCurrency(countryCode);
    }
}
