package com.yazino.web.api.payments;

import com.yazino.web.payment.creditcard.*;
import com.yazino.web.security.LogoutHelper;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.util.WebApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * This is the API equivalent of {@link com.yazino.web.payment.creditcard.CreditCardPaymentController}.
 */
@Controller
@RequestMapping("/api/1.0/payments/process/credit-card")
public class CreditCardAPIPaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(CreditCardAPIPaymentController.class);

    private final WebApiResponses webApiResponses;
    private final JsonHelper jsonHelper;
    private final LobbySessionCache lobbySessionCache;
    private final CreditCardService creditCardService;
    private final LogoutHelper logoutHelper;

    @Autowired
    public CreditCardAPIPaymentController(final WebApiResponses webApiResponses,
                                          final JsonHelper jsonHelper,
                                          final LobbySessionCache lobbySessionCache,
                                          final CreditCardService creditCardService,
                                          final LogoutHelper logoutHelper) {
        notNull(webApiResponses, "webApiResponses may not be null");
        notNull(jsonHelper, "jsonHelper may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(creditCardService, "creditCardService may not be null");
        notNull(logoutHelper, "logoutHelper may not be null");

        this.webApiResponses = webApiResponses;
        this.jsonHelper = jsonHelper;
        this.lobbySessionCache = lobbySessionCache;
        this.creditCardService = creditCardService;
        this.logoutHelper = logoutHelper;
    }

    @RequestMapping(method = RequestMethod.POST)
    public void processPayment(final HttpServletRequest request,
                               final HttpServletResponse response) throws IOException {
        notNull(request, "request may not be null");
        notNull(response, "response may not be null");

        if (!request.isSecure()) {
            LOG.debug("Request is insecure");
            webApiResponses.writeError(response, SC_BAD_REQUEST, "Request must be secure");
            return;
        }

        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session == null) {
            LOG.debug("No session present");
            webApiResponses.writeError(response, SC_UNAUTHORIZED, "No session");
            return;
        }

        final CreditCardForm form;
        try {
            form = jsonHelper.deserialize(CreditCardForm.class, request.getReader());
        } catch (Exception e) {
            LOG.debug("Request is malformed: {}", e);
            webApiResponses.writeError(response, SC_BAD_REQUEST, "Failed to deserialise request");
            return;
        }

        final List<String> errors = new ArrayList<>();
        if (!form.isValidForm(errors)) {
            LOG.debug("Request does not pass validation: {}", errors);
            webApiResponses.write(response, SC_OK, validationFailureResponseFor(errors));
            return;
        }

        final PurchaseResult details = creditCardService.completePurchase(
                form.toPaymentContext(session),
                form.toCreditCardDetails(),
                IpAddressResolver.resolveFor(request));

        switch (details.getOutcome()) {
            case PLAYER_BLOCKED:
                LOG.debug("Transaction {} resulted in player being blocked: {}; session will be terminated",
                        details.getInternalTransactionId(), session.getPlayerId());
                logoutHelper.logout(request.getSession(), request, response);
                webApiResponses.write(response, SC_OK, responseFor(details));
                return;

            default:
                LOG.debug("Transaction {} completed with outcome {} for player {}. Trace is: {}) ",
                        details.getInternalTransactionId(), details.getOutcome(), session.getPlayerId(), details.getTrace());
                webApiResponses.write(response, SC_OK, responseFor(details));
        }
    }

    private Map<String, Object> validationFailureResponseFor(final List<String> errors) {
        final Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("outcome", PurchaseOutcome.VALIDATION_ERROR.name());
        jsonResponse.put("errors", errors);
        return jsonResponse;
    }

    private Map<String, String> responseFor(final PurchaseResult result) {
        final Map<String, String> jsonResponse = new HashMap<>();
        jsonResponse.put("outcome", result.getOutcome().name());
        jsonResponse.put("internalTransactionId", result.getInternalTransactionId());
        jsonResponse.put("externalTransactionId", result.getExternalTransactionId());
        if (result.getChips() != null) {
            jsonResponse.put("chips", result.getChips().toPlainString());
        }
        return jsonResponse;
    }

}
