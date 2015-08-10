package com.yazino.web.api.payments;

import com.yazino.web.api.RequestException;
import com.yazino.web.payment.amazon.InitiatePurchaseProcessor;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

@Controller
public class PaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);
    private final WebApiResponses webApiResponses;
    private final LobbySessionCache lobbySessionCache;
    private final List<InitiatePurchaseProcessor> initiatePurchaseProcessorList;


    @Autowired
    public PaymentController(WebApiResponses webApiResponses,
                             final LobbySessionCache lobbySessionCache,
                             final List<InitiatePurchaseProcessor> initiatePurchaseProcessorList) {
        this.webApiResponses = webApiResponses;
        this.lobbySessionCache = lobbySessionCache;
        this.initiatePurchaseProcessorList = initiatePurchaseProcessorList;
    }


    @RequestMapping(value = "api/1.0/payments/{platform}/purchase/start", method = RequestMethod.POST)
    public void initiatePurchase(HttpServletRequest request,
                                 HttpServletResponse response,

                                 @RequestParam(required = false) String gameType,
                                 @RequestParam(required = false) String productId,
                                 @RequestParam(required = false) String promotionId) throws IOException {

        try {
            final LobbySession activeSession = getVerifiedSession(request);
            verifyRequestArguments(gameType, productId);

            for (InitiatePurchaseProcessor purchaseProcessor : initiatePurchaseProcessorList) {
                if(purchaseProcessor.getPlatform().equals(activeSession.getPlatform())) {
                    final Object result = purchaseProcessor.initiatePurchase(activeSession.getPlayerId(),
                                                                                 productId,
                                                                                 getSafePromotionId(promotionId),
                                                                                 gameType,
                                                                                 activeSession.getPlatform());

                    webApiResponses.writeOk(response, result);
                }
            }

            webApiResponses.writeError(response, HttpServletResponse.SC_FORBIDDEN, format("error: platform %s is unsupported", activeSession.getPlatform()));

        } catch (RequestException e) {
            webApiResponses.writeError(response, e.getHttpStatusCode(), e.getError());
        }

    }

    private LobbySession getVerifiedSession(final HttpServletRequest request) throws RequestException {
        final LobbySession activeSession = lobbySessionCache.getActiveSession(request);

        if (activeSession == null)  {
            LOG.warn("PaymentController could not load session for player {}", request);
            throw new RequestException(HttpStatus.UNAUTHORIZED.value(), "no session");
        }
        return activeSession;
    }

    private Long getSafePromotionId(final String promotionId) throws RequestException {
        if (promotionId == null)    {
            return null;
        }

        final Long promoId;
        try {
            promoId = Long.valueOf(promotionId);
        } catch (NumberFormatException e) {
            LOG.error(format("could not convert promo Id: %s to long", promotionId));
            throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, format("%s, is not a valid integer for promotion id", promotionId));
        }
        return promoId;
    }

    private void verifyRequestArguments(String gameType, String productId) throws RequestException {

        if(!StringUtils.isNotBlank(gameType))    {
            throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "error: parameter 'gameType' is missing");
        }

        if(!StringUtils.isNotBlank(productId))    {
            throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "error: parameter 'productId' is missing");
        }
    }
}
