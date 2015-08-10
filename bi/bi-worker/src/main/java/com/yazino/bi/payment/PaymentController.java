package com.yazino.bi.payment;

import com.yazino.bi.payment.worldpay.WorldPayChargebackUpdater;
import com.yazino.bi.payment.worldpay.WorldPayForexUpdater;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
@RequestMapping("/payments")
public class PaymentController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyyMMdd");

    private final WorldPayForexUpdater forexUpdater;
    private final WorldPayChargebackUpdater chargebackUpdater;

    @Autowired
    public PaymentController(final WorldPayForexUpdater forexUpdater,
                             final WorldPayChargebackUpdater chargebackUpdater) {
        notNull(forexUpdater, "forexUpdater may not be null");
        notNull(chargebackUpdater, "chargebackUpdater may not be null");

        this.forexUpdater = forexUpdater;
        this.chargebackUpdater = chargebackUpdater;
    }

    @RequestMapping("/exchange-rates/update")
    public void updateExchangeRates(final HttpServletResponse response) {
        forexUpdater.updateExchangeRates();
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @RequestMapping("/chargebacks/update")
    public void updateChargebacks(final HttpServletResponse response) {
        chargebackUpdater.updateChargebacksForYesterday();
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @RequestMapping("/chargebacks/update/{date}")
    public void updateChargebacksFor(final HttpServletResponse response,
                                     @PathVariable("date") final String date) {
        try {
            chargebackUpdater.updateChargebacksFor(DATE_FORMAT.parseDateTime(date));
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

}
