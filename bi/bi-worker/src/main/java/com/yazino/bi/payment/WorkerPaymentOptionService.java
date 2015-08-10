package com.yazino.bi.payment;

import com.google.common.base.Optional;
import com.yazino.bi.payment.persistence.JDBCPaymentOptionDAO;
import com.yazino.platform.Platform;
import com.yazino.platform.reference.Currency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Service("paymentOptionService")
public class WorkerPaymentOptionService implements PaymentOptionService {
    private final JDBCPaymentOptionDAO paymentOptionDao;

    @Autowired
    public WorkerPaymentOptionService(final JDBCPaymentOptionDAO paymentOptionDao) {
        notNull(paymentOptionDao, "paymentOptionDao may not be null");

        this.paymentOptionDao = paymentOptionDao;
    }

    @Override
    public Map<Currency, List<PaymentOption>> getAllDefaults(final Platform platform) {
        notNull(platform, "platform may not be null");

        final Map<Currency, List<PaymentOption>> options = new HashMap<Currency, List<PaymentOption>>();
        for (Currency key : Currency.values()) {
            final List<PaymentOption> paymentOptions = paymentOptionDao.findByCurrencyAndPlatform(key, platform);
            if (paymentOptions != null) {
                options.put(key, paymentOptions);
            }
        }
        return options;
    }

    @Override
    public PaymentOption getDefault(final String paymentOptionId,
                                    final Platform platform) {
        notNull(paymentOptionId, "paymentOptionId may not be null");
        notNull(platform, "platform may not be null");

        final Optional<PaymentOption> paymentOption = paymentOptionDao.findByIdAndPlatform(paymentOptionId, platform);
        if (paymentOption.isPresent()) {
            return paymentOption.get();
        }
        return null;
    }

    @Override
    public Collection<PaymentOption> getAllPaymentOptions(final Platform platform) {
        return paymentOptionDao.findByPlatform(platform);
    }

    @Override
    public Collection<PaymentOption> getAllPaymentOptions(final Currency currency,
                                                          final Platform platform) {
        notNull(platform, "platform may not be null");
        notNull(currency, "currency may not be null");

        return paymentOptionDao.findByCurrencyAndPlatform(currency, platform);
    }
}
