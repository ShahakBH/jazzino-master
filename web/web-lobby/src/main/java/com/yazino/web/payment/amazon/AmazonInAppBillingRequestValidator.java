package com.yazino.web.payment.amazon;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.yazino.web.util.RequestParameterUtils.hasParameter;

@Component
public class AmazonInAppBillingRequestValidator {

    public boolean isValidReceiptRequest(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final String userId,
                                         final String orderId,
                                         final String productId,
                                         final String gameType,
                                         final String purchaseToken) {
        return hasParameter("productId", productId, request, response)
                && hasParameter("gameType", gameType, request, response)
                && hasParameter("orderData", orderId, request, response)
                && hasParameter("purchaseToken", purchaseToken, request, response)
                && hasParameter("userId", userId, request, response);
    }

    public boolean isValidLogFailureRequest(final HttpServletRequest request,
                                            final HttpServletResponse response,
                                            final String orderId,
                                            final String gameType,
                                            final String productId,
                                            final String message) {
        return hasParameter("productId", productId, request, response) &&
                hasParameter("orderId", orderId, request, response) &&
                hasParameter("gameType", gameType, request, response) &&
                hasParameter("productId", productId, request, response) &&
                hasParameter("message", message, request, response);
    }
}
