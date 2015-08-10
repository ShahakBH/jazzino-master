package com.yazino.web.payment.itunes;

import com.google.common.collect.Lists;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.platform.Partner;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponseWriter;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.*;

import static com.yazino.web.payment.itunes.AppStoreConstants.*;

@Controller("itunesCashierController")
@AllowPublicAccess
@RequestMapping("/payment/itunes/*")
public class AppStoreController {
    private static final Logger LOG = LoggerFactory.getLogger(AppStoreController.class);

    static final Map<String, List<AppStoreChipPackage>> EMPTY_CHIP_PACKAGES
            = toChipPackageMap(Collections.<AppStoreChipPackage>emptyList());
    static final Map<String, Set<String>> EMPTY_AVAILABLE_PRODUCTS
            = toAllProductsMap(Collections.<String>emptySet(), Collections.<String>emptySet());

    private final LobbySessionCache mLobbySessionCache;
    private final AppStoreService mAppStoreService;

    private AppStoreChipPurchaseTransformer mChipPurchaseTransformer = new AppStoreChipPurchaseTransformer();
    private WebApiResponses mResponseWriter = defaultJSONWriter();

    @Autowired
    public AppStoreController(final LobbySessionCache lobbySessionCache,
                              final AppStoreService appStoreService) {
        Validate.noNullElements(new Object[]{lobbySessionCache, appStoreService});
        mLobbySessionCache = lobbySessionCache;
        mAppStoreService = appStoreService;
    }

    // gameIdentifier is either a server side identifier (SLOTS/BLACKJACK etc) or an ios bundle identifier,
    @RequestMapping("allProducts")
    public void fetchAvailableProducts(final HttpServletResponse response,
                                       @RequestParam(GAME_TYPE) final String gameIdentifier) throws IOException {
        final AppStoreConfiguration configuration = mAppStoreService.getConfiguration();

        final Set<String> standardProducts = configuration.standardProductsForGame(gameIdentifier);
        final Set<String> promotionProducts = configuration.promotionProductsForGame(gameIdentifier);
        if (standardProducts.isEmpty() && promotionProducts.isEmpty()) {
            mResponseWriter.writeOk(response, EMPTY_AVAILABLE_PRODUCTS);
            return;
        }
        final Map<String, Set<String>> map = toAllProductsMap(standardProducts, promotionProducts);

        mResponseWriter.writeOk(response, map);
    }

    // gameIdentifier is either a server side identifier (SLOTS/BLACKJACK etc) or an ios bundle identifier,
    @RequestMapping("chipPackages")
    public void fetchChipPackages(final HttpServletResponse response,
                                  @RequestParam(GAME_TYPE) final String gameIdentifier,
                                  @RequestParam(PLAYER_ID) final String playerIdAsString) throws IOException {
        final BigDecimal playerId = parseBigDecimal(playerIdAsString);
        if (playerId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final List<AppStoreChipPackage> packages;
        try {
            packages = findChipPackagesForPlayer(gameIdentifier, playerId);
            if (packages.isEmpty()) {
                mResponseWriter.writeOk(response, EMPTY_CHIP_PACKAGES);
                return;
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid player ID submitted: {}", playerIdAsString);
            mResponseWriter.writeOk(response, EMPTY_CHIP_PACKAGES);
            return;
        }

        final Map<String, List<AppStoreChipPackage>> wrapped = toChipPackageMap(packages);

        LOG.debug("Found [{}] packages for player [{}], sending [{}]", packages.size(), playerId, wrapped);

        mResponseWriter.writeOk(response, wrapped);
    }

    private BigDecimal parseBigDecimal(final String bigDecimalAsString) {
        try {
            return new BigDecimal(bigDecimalAsString);
        } catch (NumberFormatException e) {
            LOG.error("Received invalid bigdecimal: {}", bigDecimalAsString);
            return null;
        }
    }

    @RequestMapping("transactionReceipt")
    public void processPayment(final HttpServletRequest request,
                               final HttpServletResponse response,
                               @RequestParam(GAME_TYPE) final String gameIdentifier,
                               @RequestParam(CURRENCY) final String currencyCode,
                               @RequestParam(AMOUNT_CASH) final String amountCashAsString,
                               @RequestParam(PRODUCT_IDENTIFIER) final String productIdentifier,
                               @RequestParam(TRANSACTION_IDENTIFIER) final String transactionIdentifier,
                               @RequestParam(TRANSACTION_RECEIPT) final String transactionReceipt) throws Exception {
        final BigDecimal amountCash = parseBigDecimal(amountCashAsString);
        if (amountCash == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final LobbySession lobbySession = mLobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            response.sendError(NOT_FOUND_ERROR);
            return;
        }

        final BigDecimal playerId = lobbySession.getPlayerId();
        final Partner partnerId = lobbySession.getPartnerId();
        final Currency currency = Currency.getInstance(currencyCode);

        final AppStorePaymentContext context = new AppStorePaymentContext(playerId, lobbySession.getSessionId(), gameIdentifier,
                transactionReceipt, amountCash, currency, transactionIdentifier, productIdentifier, partnerId);
        final AppStoreOrder order = mAppStoreService.fulfilOrder(context);

        final AppStoreChipPurchaseResult result = mChipPurchaseTransformer.transform(order);
        mResponseWriter.writeOk(response, result);
    }

    @Deprecated
    @RequestMapping("products")
    public void getProducts(final HttpServletResponse response,
                            @RequestParam(GAME_TYPE) final String gameType) throws IOException {

        final PrintWriter writer = response.getWriter();
        if (gameType.equalsIgnoreCase("SLOTS")) {
            writer.write("{\"products\":{\"USD15_BUYS_30K\":{\"chips\":30000},\"USD70_BUYS_200K\":{\"chips\":200000},"
                    + "\"USD30_BUYS_70K\":{\"chips\":70000},\"USD3_BUYS_5K\":{\"chips\":5000},"
                    + "\"USD90_BUYS_300K_2\":{\"chips\":300000},\"USD8_BUYS_15K\":{\"chips\":15000}}}");
        } else if (gameType.equalsIgnoreCase("BLACKJACK")) {
            writer.write("{\"products\":{\"BLACKJACK_USD70_BUYS_200K\":{\"chips\":200000},"
                    + "\"BLACKJACK_USD8_BUYS_15K\":{\"chips\":15000},"
                    + "\"BLACKJACK_USD15_BUYS_30K_1\":{\"chips\":30000},"
                    + "\"BLACKJACK_USD90_BUYS_300K_1\":{\"chips\":300000},"
                    + "\"BLACKJACK_USD3_BUYS_5K\":{\"chips\":5000},"
                    + "\"BLACKJACK_USD30_BUYS_70K\":{\"chips\":70000}}}");
        }
        writer.flush();
    }

    @Deprecated
    @RequestMapping("productIdentifiers")
    public void getProductIdentifiers(final HttpServletResponse response,
                                      @RequestParam(value = GAME_TYPE, defaultValue = "SLOTS") final String gameType)
            throws IOException {

        final PrintWriter writer = response.getWriter();

        if (gameType.equalsIgnoreCase("SLOTS")) {
            writer.write("[\"USD3_BUYS_5K\",\"USD8_BUYS_15K\",\"USD15_BUYS_30K\",\"USD30_BUYS_70K\","
                    + "\"USD70_BUYS_200K\",\"USD90_BUYS_300K_2\"]");
        } else if (gameType.equalsIgnoreCase("BLACKJACK")) {
            writer.write("[\"BLACKJACK_USD3_BUYS_5K\",\"BLACKJACK_USD8_BUYS_15K\","
                    + "\"BLACKJACK_USD15_BUYS_30K_1\",\"BLACKJACK_USD30_BUYS_70K\","
                    + "\"BLACKJACK_USD70_BUYS_200K\",\"BLACKJACK_USD90_BUYS_300K_1\"]");
        }
        writer.flush();
    }

    private List<AppStoreChipPackage> findChipPackagesForPlayer(final String gameType,
                                                                final BigDecimal playerId) {
        final Map<String, String> optionMappings
                = mAppStoreService.getConfiguration().productIdentifierMappingsForGame(gameType);
        if (optionMappings.isEmpty()) {
            return Collections.emptyList();
        }

        final List<PaymentOption> options = mAppStoreService.paymentOptionsForPlayer(playerId);

        return transformToPackages(optionMappings, options);
    }

    List<AppStoreChipPackage> transformToPackages(final Map<String, String> mappings,
                                                  final List<PaymentOption> options) {
        final AppStoreConfiguration configuration = mAppStoreService.getConfiguration();
        final AppStorePaymentOptionTransformer transformer = new AppStorePaymentOptionTransformer(mappings);
        transformer.setStandardProductHeaders(configuration.getStandardHeaders());
        transformer.setStandardProductSubHeaders(configuration.getStandardSubHeaders());

        final List<AppStoreChipPackage> packages = Lists.transform(options, transformer);

        LOG.debug("Transformed [{}] options into [{}]", options, packages);

        return packages;
    }

    final void setResponseWriter(final WebApiResponses responseWriter) {
        Validate.notNull(responseWriter);
        mResponseWriter = responseWriter;
    }

    final void setChipPurchaseTransformer(final AppStoreChipPurchaseTransformer chipPurchaseTransformer) {
        Validate.notNull(chipPurchaseTransformer);
        mChipPurchaseTransformer = chipPurchaseTransformer;
    }

    private static WebApiResponses defaultJSONWriter() {
        return new WebApiResponses(new WebApiResponseWriter(true));
    }

    private static Map<String, List<AppStoreChipPackage>> toChipPackageMap(final List<AppStoreChipPackage> packages) {
        final Map<String, List<AppStoreChipPackage>> wrapped = new HashMap<>(1);
        wrapped.put(CHIP_PACKAGES, packages);
        return wrapped;
    }

    private static Map<String, Set<String>> toAllProductsMap(final Set<String> standardProducts,
                                                             final Set<String> promotionProducts) {
        final Map<String, Set<String>> wrapped = new HashMap<>(2);
        wrapped.put(STANDARD_PRODUCTS, standardProducts);
        wrapped.put(PROMOTION_PRODUCTS, promotionProducts);
        return wrapped;
    }

}
