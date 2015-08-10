package com.yazino.web.api.payments;

import com.yazino.platform.Platform;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.api.RequestException;
import com.yazino.web.payment.ProductService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/**
 * Handles product requests.<br/>
 * Currently supported platforms are: FACEBOOK_CANVAS.
 */
@Controller
@AllowPublicAccess("/api/1.0/payments/products/**")
public class ProductController {
    private static final Logger LOG = LoggerFactory.getLogger(ProductController.class);

    private final LobbySessionCache lobbySessionCache;
    private final WebApiResponses responseWriter;
    private final CookieHelper cookieHelper;
    @Resource
    private Map<Platform, ProductService> productServices;

    void setProductServices(Map<Platform, ProductService> productServices) {
        this.productServices = productServices;
    }

    @Autowired
    public ProductController(LobbySessionCache lobbySessionCache,
                             WebApiResponses responseWriter,
                             CookieHelper cookieHelper) {
        this.lobbySessionCache = lobbySessionCache;
        this.responseWriter = responseWriter;
        this.cookieHelper = cookieHelper;
    }

    /**
     * Retrieves the available chip products for current player and platform. A {@link ChipProducts} is written to the response. Products within the response
     * are ordered by chips descending (not promoted chips).
     *
     * @param request
     * @param response
     * @param gameType
     * @throws IOException
     */
    @RequestMapping(value = "/api/1.0/payments/products/{platform}")
    public void products(HttpServletRequest request,
                         HttpServletResponse response,
                         @PathVariable("platform") final String platformInput,
                         @RequestParam(value = "gameType", required = true) final String gameType) throws IOException {
        LOG.debug("Requesting facebook products, gameType: {}", gameType);

        try {
            Platform platform = verifyPlatform(platformInput);
            verifyProductRequest(request, response, gameType, platform);
            LobbySession session = verifySession(request);

            ProductService productService = productServices.get(platform);
            ChipProducts products = productService.getAvailableProducts(request, session.getPlayerId(), gameType);

            orderProducts(products);
            responseWriter.writeOk(response, products);
        } catch (RequestException e) {
            safeSendError(response, e.getHttpStatusCode(), e.getError());
        }
    }

    private Platform verifyPlatform(String platformInput) throws RequestException {
        Platform platform = Platform.safeValueOf(platformInput);
        if (platform == null || !productServices.containsKey(platform)) {
            throw new RequestException(HttpServletResponse.SC_FORBIDDEN, String.format("Unsupported platform: '%s'", platformInput));
        }
        return platform;
    }

    private void orderProducts(ChipProducts products) {
        Collections.sort(products.getChipProducts(), new Comparator<ChipProduct>() {
            @Override
            public int compare(ChipProduct p1, ChipProduct p2) {
                return p2.getChips().compareTo(p1.getChips());
            }
        });
    }

    private void verifyProductRequest(HttpServletRequest request, HttpServletResponse response, String gameType, Platform platform) throws RequestException {
        if (platform == Platform.FACEBOOK_CANVAS) {
            boolean onCanvas = cookieHelper.isOnCanvas(request, response);
            if (!onCanvas) {
                LOG.warn("Invalid request for facebook products. Only available to canvas apps.");
                throw new RequestException(HttpServletResponse.SC_FORBIDDEN, "facebook payment only available to canvas apps");
            }
        }

        if (StringUtils.isBlank(gameType)) {
            LOG.warn("Invalid request for '{}' products. Parameter 'gameType' is blank or null.", platform.name());
            throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "parameter 'gameType' is missing");
        }
    }

    private LobbySession verifySession(HttpServletRequest request) throws RequestException {
        LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session == null) {
            LOG.info("No active session when requesting FACEBOOK products");
            throw new RequestException(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - no session");
        }
        return session;
    }

    private void safeSendError(HttpServletResponse response, int httpCode, String errorMessage) {
        try {
            responseWriter.writeError(response, httpCode, errorMessage);
        } catch (IOException e) {
            LOG.error("failed to send error to client. http error code: {}", httpCode);
        }
    }
}
