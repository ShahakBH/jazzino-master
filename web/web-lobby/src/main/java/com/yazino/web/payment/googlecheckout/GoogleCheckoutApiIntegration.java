package com.yazino.web.payment.googlecheckout;

import com.google.checkout.sdk.commands.ApiContext;
import com.google.checkout.sdk.commands.CheckoutException;
import com.google.checkout.sdk.domain.*;
import com.google.checkout.sdk.util.HttpUrlException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.util.*;

import static com.google.checkout.sdk.commands.EnvironmentInterface.CommandType.ORDER_PROCESSING;
import static com.google.checkout.sdk.commands.EnvironmentInterface.CommandType.REPORTS;

/**
 * Current integration only extends to verifying orders place by in app billing via Google Play. Assumption: that the
 * app will never placed more than one item in the basket.
 */
@Service("googleCheckoutApiIntegration")
public class GoogleCheckoutApiIntegration {
    private static final Logger LOG = LoggerFactory.getLogger(GoogleCheckoutApiIntegration.class);

    private final GoogleCheckoutContext googleCheckoutContext;

    @Autowired
    public GoogleCheckoutApiIntegration(final GoogleCheckoutContext googleCheckoutContext) {
        Validate.notNull(googleCheckoutContext);
        this.googleCheckoutContext = googleCheckoutContext;
        LOG.debug("GoogleCheckoutApiIntegration configured with api context: {}", googleCheckoutContext);
    }

    /**
     * Fetches notifications and order summary from google checkout for the given order number.
     *
     * @param orderNumber order of interest
     * @return summary of current order. The order's state will be one of:
     *         <ul><li>INVALID_ORDER_NUMBER</li>
     *         <li>PAYMENT_AUTHORIZED</li>
     *         <li>PAYMENT_NOT_AUTHORIZED</li>
     *         <li>CANCELLED</li>
     *         <li>DELIVERED</li>
     *         <li>ERROR</li>
     *         </ul>
     * @see Order.Status
     */
    public Order retrieveOrderState(final String orderNumber) {
        Validate.notBlank(orderNumber, "orderNumber cannot be null or empty");
        LOG.debug("Requesting notifications for order {}", orderNumber);
        final Order order = new Order(orderNumber, Order.Status.ERROR);

        try {
            final NotificationHistoryResponse notificationHistory = fetchNotificationHistoryFor(orderNumber);
            if (isOrderUnKnownToGoogle(notificationHistory)) {
                order.setStatus(Order.Status.INVALID_ORDER_NUMBER);
                return order;
            }
            final NotificationHistoryResponse.Notifications notifications = notificationHistory.getNotifications();
            if (notifications != null) {
                setOrderStatus(order, notifications);
                setOrderPurchaseInfo(order, notifications);
            }
        } catch (HttpUrlException hue) {
            // given we're using Google's sdk to build requests, the only way we'll get a 400, is if the order number is
            // not valid. e.g. not an integer. The docs are a bit weak on this, but this seemd to be the case, anyway
            // we can't deal with it.
            if (HttpURLConnection.HTTP_BAD_REQUEST == hue.getCode()) {
                order.setStatus(Order.Status.INVALID_ORDER_NUMBER);
            }
            LOG.error("Failed to get order notifications for order: {}", orderNumber, hue);
        } catch (CheckoutException ce) {
            LOG.error("Failed to get order notifications for order: {}", orderNumber, ce);
        }

        return order;
    }

    /**
     * Makes request to change the fulfillment state of an order to DELIVERED.
     *
     * @param orderNumber order number to flag as delivered
     * @return true if request was made successfully. This does not imply the state has been changed.
     */
    public boolean markOrderAsDelivered(final String orderNumber) {
        Validate.notBlank(orderNumber, "orderNumber cannot be null or empty");
        LOG.debug("Marking order {} as delivered", orderNumber);

        final DeliverOrderRequest request = GoogleCheckoutApiUtils.buildDeliverOrderRequest(orderNumber);
        try {
            logRequest(orderNumber, request);
            apiContext().postCommand(ORDER_PROCESSING, request.toJAXB());
            return true;
        } catch (CheckoutException e) {
            LOG.error("Failed to change order state to delivered for order: {}", orderNumber, e);
        }
        return false;
    }

    private void setOrderPurchaseInfo(final Order order,
                                      final NotificationHistoryResponse.Notifications notifications) {
        final NewOrderNotification newOrderNotification = findNewOrderNotification(notifications);
        final ShoppingCart shoppingCart = newOrderNotification.getShoppingCart();
        final List<Item> items = shoppingCart.getItems().getItem();
        if (items.isEmpty() || items.size() > 1) {
            LOG.error("Order number {}, must have exactly 1 item in cart", newOrderNotification.getGoogleOrderNumber());
            order.setStatus(Order.Status.ERROR);
            return;
        }
        final Item item = items.get(0);
        // extract order value and currency
        final Money price = item.getUnitPrice();
        order.setCurrencyCode(price.getCurrency());
        order.setPrice(price.getValue());
        // and product id
        final String productId = extractProductId(item.getMerchantItemId());
        order.setProductId(productId);
    }

    private String extractProductId(final String merchantItemId) {
        if (StringUtils.isNotBlank(merchantItemId)) {
            final int lastColon = merchantItemId.lastIndexOf(':');
            if (lastColon == -1) {
                return merchantItemId;
            } else {
                return merchantItemId.substring(Math.min(merchantItemId.length(), lastColon + 1));
            }
        }
        return "";
    }

    /**
     * Fetches the notification history from google for the given order.
     *
     * @param orderNumber order to get notifications for
     * @return notifications or null if api throws CheckoutException
     * @throws CheckoutException see {@link ApiContext#postCommand(
     *com.google.checkout.sdk.commands.EnvironmentInterface.CommandType, javax.xml.bind.JAXBElement)}
     */
    private NotificationHistoryResponse fetchNotificationHistoryFor(final String orderNumber) throws CheckoutException {
        final NotificationHistoryRequest request = GoogleCheckoutApiUtils.buildNotificationHistoryRequest(orderNumber);

        logRequest(orderNumber, request);
        final NotificationHistoryResponse response = (NotificationHistoryResponse) apiContext().postCommand(REPORTS,
                request.toJAXB());
        logResponse(orderNumber, response);
        return response;
    }

    public ApiContext apiContext() {
        return googleCheckoutContext.getApiContext();
    }

    private void logResponse(final String orderNumber, final NotificationHistoryResponse response) {
        LOG.debug("Notify response for order number: {}, is: {}", orderNumber, response.toString());
    }

    private void logRequest(final String orderNumber, final NotificationHistoryRequest request) {
        LOG.debug("Notify request for order number: {}, is: {}", orderNumber, request.toString());
    }

    private void logRequest(final String orderNumber, final DeliverOrderRequest request) {
        LOG.debug("Deliver order request for order number: {}, is: {}", orderNumber, request.toString());
    }

    private boolean isOrderUnKnownToGoogle(final NotificationHistoryResponse response) {
        final NotificationHistoryResponse.InvalidOrderNumbers invalidOrderNumbers = response.getInvalidOrderNumbers();
        return invalidOrderNumbers != null && CollectionUtils.isNotEmpty(invalidOrderNumbers.getGoogleOrderNumber());
    }

    private void setOrderStatus(final Order order,
                                final NotificationHistoryResponse.Notifications notifications) {
        final OrderStateChangeNotification latestNotification
                = findLatestStateChangeNotification(notifications);
        if (latestNotification == null) {
            // this can happen if order is new, should never happen with Google PLay but hey ho
            order.setStatus(Order.Status.PAYMENT_NOT_AUTHORIZED);
        } else {
            final String googleOrderNumber = latestNotification.getGoogleOrderNumber();
            final FinancialOrderState latestFinancialOrderState = latestNotification.getNewFinancialOrderState();
            LOG.debug("Google checkout order: {}, latest financial state is {}"
                    , googleOrderNumber, latestFinancialOrderState.name());
            switch (latestFinancialOrderState) {
                case CHARGED:
                case CHARGING:
                case CHARGEABLE:
                    order.setStatus(Order.Status.PAYMENT_AUTHORIZED);
                    break;
                case REVIEWING:
                    // shouldn't happen with GooglePlay
                    order.setStatus(Order.Status.PAYMENT_NOT_AUTHORIZED);
                    break;
                case PAYMENT_DECLINED:
                case CANCELLED:
                case CANCELLED_BY_GOOGLE:
                    // shouldn't happen with GooglePlay
                    order.setStatus(Order.Status.CANCELLED);
                    break;
                default:
                    order.setStatus(Order.Status.ERROR);
                    LOG.error("Unknown state for Google checkout order: {}, latest financial state is {}"
                            , googleOrderNumber, latestFinancialOrderState.name());
                    break;
            }
        }
    }

    private OrderStateChangeNotification findLatestStateChangeNotification(
            final NotificationHistoryResponse.Notifications notifications) {
        final Collection<OrderStateChangeNotification> stateChangeNotifications
                = selectOrderStateNotifications(notifications);
        if (stateChangeNotifications.isEmpty()) {
            return null;
        }
        final List<OrderStateChangeNotification> orderedNotifications = orderByTimestampDescending(
                stateChangeNotifications);
        return orderedNotifications.get(0);
    }

    private List<OrderStateChangeNotification> orderByTimestampDescending(
            final Collection<OrderStateChangeNotification> orderStateChangedNotifications) {
        final List<OrderStateChangeNotification> orderedNotifications
                = new ArrayList<OrderStateChangeNotification>(orderStateChangedNotifications);
        Collections.sort(orderedNotifications, new Comparator<OrderStateChangeNotification>() {
            @Override
            public int compare(final OrderStateChangeNotification n1, final OrderStateChangeNotification n2) {
                return n2.getTimestamp().compare(n1.getTimestamp());
            }
        });
        return orderedNotifications;
    }

    @SuppressWarnings("unchecked")
    private Collection<OrderStateChangeNotification> selectOrderStateNotifications(
            final NotificationHistoryResponse.Notifications notifications) {
        final List orderStateChangedNotifications = new ArrayList(notifications.getAllNotifications());
        CollectionUtils.filter(orderStateChangedNotifications, new Predicate() {
            @Override
            public boolean evaluate(final Object o) {
                return o instanceof OrderStateChangeNotification;
            }
        });
        return orderStateChangedNotifications;
    }

    private NewOrderNotification findNewOrderNotification(
            final NotificationHistoryResponse.Notifications notifications) {
        return (NewOrderNotification) CollectionUtils.find(notifications.getAllNotifications(),
                new Predicate() {
                    @Override
                    public boolean evaluate(final Object o) {
                        return o instanceof NewOrderNotification;
                    }
                });
    }
}
