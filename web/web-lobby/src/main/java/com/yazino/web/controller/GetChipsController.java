package com.yazino.web.controller;


import com.yazino.bi.payment.PaymentOptionService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.reference.Currency;
import com.yazino.web.data.CurrencyRepository;
import com.yazino.web.domain.CashierConfigurationContainer;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class GetChipsController {
    private static final String VIEW_NAME = "buyChips";
    private static final String URL_MAPPING = "getChips";

    private final PlayerService playerService;
    private final LobbySessionCache lobbySessionCache;
    private final PaymentOptionService paymentOptionService;
    private final CashierConfigurationContainer cashierConfiguration;
    private final CookieHelper cookieHelper;
    private final SiteConfiguration siteConfiguration;
    private final CurrencyRepository currencyRepository;

    @Autowired
    public GetChipsController(final PlayerService playerService,
                              @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
                              final PaymentOptionService paymentOptionService,
                              @Qualifier("cashierConfiguration") final CashierConfigurationContainer cashierConfiguration,
                              @Qualifier("cookieHelper") final CookieHelper cookieHelper,
                              @Qualifier("siteConfiguration") final SiteConfiguration siteConfiguration,
                              final CurrencyRepository currencyRepository) {
        notNull(playerService, "playerService may not be null");
        notNull(lobbySessionCache, "Lobby Session Cache may not be null");
        notNull(paymentOptionService, "paymentOptionService may not be null");
        notNull(cashierConfiguration, "Cashier configuration may not be null");
        notNull(cookieHelper, "cookieHelper may not be null");
        notNull(siteConfiguration, "siteConfiguration may not be null");
        notNull(currencyRepository, "currencyRepository may not be null");

        this.playerService = playerService;
        this.lobbySessionCache = lobbySessionCache;
        this.paymentOptionService = paymentOptionService;
        this.cashierConfiguration = cashierConfiguration;
        this.cookieHelper = cookieHelper;
        this.siteConfiguration = siteConfiguration;
        this.currencyRepository = currencyRepository;
    }

    private Map<String, String> createAcceptedCurrenciesMap() {
        final Map<String, String> acceptedCurrencies
                = new LinkedHashMap<String, String>(Currency.values().length);
        for (Currency acceptedCurrency : currencyRepository.getAcceptedCurrencies()) {
            acceptedCurrencies.put(acceptedCurrency.getCode(), acceptedCurrency.getDescription());
        }
        return acceptedCurrencies;
    }

    private void populateCommonViewDetails(final HttpServletRequest request, final ModelMap modelMap) {
        populateCommonViewDetails(request, modelMap, null);
    }

    private void populateCommonViewDetails(final HttpServletRequest request,
                                           final ModelMap modelMap,
                                           final String preferredCurrencyString) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            throw new RuntimeException("No lobby session found");
        }

        final BigDecimal playerId = lobbySession.getPlayerId();

        final PaymentPreferences paymentPreferences = playerService.getPaymentPreferences(playerId);

        Currency currency = null;
        if (paymentPreferences != null) {
            currency = paymentPreferences.getCurrency();
        }

        Currency preferredCurrency;
        if (preferredCurrencyString == null) {
            preferredCurrency = currency;
        } else {
            preferredCurrency = Currency.valueOf(preferredCurrencyString);
        }
        modelMap.addAttribute("paymentOptions", paymentOptionService.getAllPaymentOptions(preferredCurrency, lobbySession.getPlatform()));
        modelMap.addAttribute("cashierPopUp", Boolean.TRUE);
        modelMap.addAttribute("cashierConfiguration", cashierConfiguration);
        modelMap.addAttribute("acceptedCurrencies", createAcceptedCurrenciesMap());
        if (preferredCurrency != null) {
            modelMap.addAttribute("preferredCurrency", preferredCurrency.toString());
        }
        modelMap.addAttribute("controllerUrl", URL_MAPPING);
    }

    @RequestMapping(value = {"/lobby/" + URL_MAPPING, "/" + URL_MAPPING}, method = RequestMethod.POST)
    public String getChipsSubmit(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final ModelMap modelMap,
                                 final String updatedCurrency,
                                 @RequestParam(value = "gameType", required = false) final String gameType) {
        populateCommonViewDetails(request, modelMap, updatedCurrency);

        updatePaymentGameType(request, response, gameType);

        return VIEW_NAME;
    }

    @RequestMapping(value = {"/lobby/" + URL_MAPPING, "/" + URL_MAPPING}, method = RequestMethod.GET)
    public String getChips(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final ModelMap modelMap,
                           @RequestParam(value = "gameType", required = false) final String gameType) {
        populateCommonViewDetails(request, modelMap);

        updatePaymentGameType(request, response, gameType);

        return VIEW_NAME;
    }

    private void updatePaymentGameType(final HttpServletRequest request,
                                       final HttpServletResponse response,
                                       final String gameType) {
        String currentGameType = gameType;
        if (currentGameType == null) {
            currentGameType = cookieHelper.getLastGameType(request.getCookies(),
                    siteConfiguration.getDefaultGameType());
        }
        cookieHelper.setPaymentGameType(response, currentGameType);
    }
}
