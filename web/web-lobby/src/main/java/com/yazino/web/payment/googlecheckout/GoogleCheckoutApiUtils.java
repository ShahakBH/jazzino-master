package com.yazino.web.payment.googlecheckout;

import com.google.checkout.sdk.domain.DeliverOrderRequest;
import com.google.checkout.sdk.domain.NotificationHistoryRequest;

public final class GoogleCheckoutApiUtils {
    private GoogleCheckoutApiUtils() {
    }

    // notification types of interest
    public static final String ORDER_STATE_CHANGE = "order-state-change";
    public static final String NEW_ORDER = "new-order";

    public static DeliverOrderRequest buildDeliverOrderRequest(final String orderNumber) {
        final DeliverOrderRequest request = new DeliverOrderRequest();
        request.setGoogleOrderNumber(orderNumber);
        return request;
    }

    /**
     * Creates a NotificationHistoryRequest requesting {@code}order-state-change and {@code}new-order notifications
     * for an order.
     *
     * @param orderNumber order to request notifications for
     * @return the request
     */
    public static NotificationHistoryRequest buildNotificationHistoryRequest(final String orderNumber) {
        final NotificationHistoryRequest request = new NotificationHistoryRequest();
        final NotificationHistoryRequest.OrderNumbers numbers = new NotificationHistoryRequest.OrderNumbers();
        numbers.getGoogleOrderNumber().add(orderNumber);
        request.setOrderNumbers(numbers);
        final NotificationHistoryRequest.NotificationTypes notificationTypes
                = new NotificationHistoryRequest.NotificationTypes();
        notificationTypes.getNotificationType().add(ORDER_STATE_CHANGE);
        notificationTypes.getNotificationType().add(NEW_ORDER);
        request.setNotificationTypes(notificationTypes);
        return request;
    }
}
