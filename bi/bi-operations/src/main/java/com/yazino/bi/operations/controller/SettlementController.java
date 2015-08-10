package com.yazino.bi.operations.controller;

import com.yazino.platform.payment.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;

import static java.lang.String.format;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class SettlementController {
    private static final int PAGE_SIZE = 20;

    private final PaymentService paymentService;

    @Autowired
    public SettlementController(final PaymentService paymentService) {
        notNull(paymentService, "paymentService may not be null");

        this.paymentService = paymentService;
    }

    @RequestMapping(value = "/payments/pending")
    public ModelAndView pendingSettlements() {
        return pendingSettlements(1);
    }

    @RequestMapping(value = "/payments/pending/{page}")
    public ModelAndView pendingSettlements(@PathVariable("page") final int page) {
        return new ModelAndView("payments/pending")
                .addObject("pendingSettlements", paymentService.findAuthorised(page - 1, PAGE_SIZE));
    }

    @RequestMapping(value = "/payments/cancelForPlayer/{playerId:.+}")
    public ModelAndView cancelForPlayer(@PathVariable("playerId") final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        final int cancelledCount = paymentService.cancelAllSettlementsForPlayer(playerId);

        return pendingSettlements()
                .addObject("message", format("Player %s has been blocked and %d payments were cancelled", playerId, cancelledCount));
    }

}
