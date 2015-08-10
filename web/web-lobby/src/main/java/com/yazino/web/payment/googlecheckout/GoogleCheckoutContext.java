package com.yazino.web.payment.googlecheckout;


import com.google.checkout.sdk.commands.ApiContext;
import com.google.checkout.sdk.commands.CheckoutException;
import com.google.checkout.sdk.commands.Environment;
import com.google.checkout.sdk.commands.EnvironmentInterface;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;

import static com.google.checkout.sdk.commands.EnvironmentInterface.CommandType.*;

@Component("googleCheckoutContext")
public class GoogleCheckoutContext {
    private final Environment environment;
    private final YazinoApiContext apiContext;

    /**
     * ApiContext is subclassed simply to override the default read and connect timeouts. These default to zero
     * (i.e. infinite timeout).
     */
    private final class YazinoApiContext extends ApiContext {

        private final int readTimeout;

        private final int connectTimeOut;

        /**
         * Creates a new ApiContext.
         *
         * @param environment    The environment, such as {@link com.google.checkout.sdk.commands.Environment#SANDBOX}
         *                       or
         *                       {@link com.google.checkout.sdk.commands.Environment#PRODUCTION}, which should be used.
         * @param merchantId     The Merchant's ID, as taken from the Checkout Integration
         *                       Settings webpage.
         * @param merchantKey    The Merchant's Key, as taken from the Checkout Integration
         *                       Settings webpage.
         * @param currencyCode   The currency-code in which this merchant operates, such
         *                       as USD for United States Dollars, or GBP for Great Britain Pounds.
         * @param readTimeout    HttpURLConnection read timeout in millisecs
         * @param connectTimeOut HttpURLConnection connect timeout in millisecs
         * @see <a href="https://sandbox.google.com/checkout/sell/settings?section=Integration">
         *     Sandbox Integration Console</a>
         * @see <a href="https://checkout.google.com/sell/settings?section=Integration">
         *     Production Integration Console</a>
         */
        private YazinoApiContext(final EnvironmentInterface environment,
                                 final String merchantId,
                                 final String merchantKey,
                                 final String currencyCode,
                                 final int readTimeout,
                                 final int connectTimeOut) {
            super(environment, merchantId, merchantKey, currencyCode);
            this.readTimeout = readTimeout;
            this.connectTimeOut = connectTimeOut;
        }

        @Override
        public HttpURLConnection makeConnection(final String toUrl) throws CheckoutException {
            final HttpURLConnection httpURLConnection = super.makeConnection(toUrl);
            httpURLConnection.setReadTimeout(readTimeout);
            httpURLConnection.setConnectTimeout(connectTimeOut);
            return httpURLConnection;
        }
    }

    @Autowired
    public GoogleCheckoutContext(@Value("${googlecheckout.environment}") final String environmentName,
                                 @Value("${googlecheckout.merchant.id}") final String merchantId,
                                 @Value("${googlecheckout.merchant.key}") final String merchantKey,
                                 @Value("${googlecheckout.merchant.currency}") final String merchantCurrency,
                                 @Value("${googlecheckout.read.timeout}") final String readTimeout,
                                 @Value("${googlecheckout.connect.timeout}") final String connectTimeout) {
        Validate.notBlank(environmentName, "environment name must be either SANDBOX or PRODUCTION");
        Validate.matchesPattern(environmentName.toUpperCase(), "^SANDBOX|PRODUCTION$");
        Validate.notBlank(merchantId, "merchantId cannot be null or empty");
        Validate.notBlank(merchantKey, "merchantKey cannot be null or empty");
        Validate.notBlank(merchantCurrency, "merchantCurrency cannot be null or empty");
        Validate.isTrue(Integer.parseInt(readTimeout) >= 0, "read timeout must be >= 0");
        Validate.isTrue(Integer.parseInt(connectTimeout) >= 0, "read timeout must be >= 0");
        this.environment = selectEnvironment(environmentName);
        this.apiContext = new YazinoApiContext(environment, merchantId, merchantKey, merchantCurrency,
                Integer.parseInt(readTimeout), Integer.parseInt(connectTimeout));
    }

    private Environment selectEnvironment(final String environmentName) {
        if ("PRODUCTION".equalsIgnoreCase(environmentName)) {
            return Environment.PRODUCTION;
        }
        return Environment.SANDBOX;
    }

    public ApiContext getApiContext() {
        return apiContext;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE)
                .append(environment.getUrl(ORDER_PROCESSING, apiContext.getMerchantId()))
                .append(environment.getUrl(REPORTS, apiContext.getMerchantId()))
                .append(environment.getUrl(CART_POST, apiContext.getMerchantId()))
                .append(apiContext.getMerchantId())
                .append(apiContext.getMerchantKey())
                .append(apiContext.getMerchantCurrencyCode())
                .toString();
    }
}
