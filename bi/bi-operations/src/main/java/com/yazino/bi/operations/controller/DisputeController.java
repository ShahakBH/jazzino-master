package com.yazino.bi.operations.controller;

import com.yazino.platform.payment.DisputeResolution;
import com.yazino.platform.payment.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import static java.lang.String.format;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class DisputeController {
    private static final int PAGE_SIZE = 20;

    private final PaymentService paymentService;

    @Autowired
    public DisputeController(final PaymentService paymentService) {
        notNull(paymentService, "paymentService may not be null");

        this.paymentService = paymentService;
    }

    @RequestMapping(value = "/payments/disputes/open")
    public ModelAndView openDisputes() {
        return openDisputes(1);
    }

    @RequestMapping(value = "/payments/disputes/open/{page}")
    public ModelAndView openDisputes(@PathVariable("page") final int page) {
        return new ModelAndView("payments/disputes")
                .addObject("disputes", paymentService.findOpenDisputes(page - 1, PAGE_SIZE));
    }

    @RequestMapping(value = "/payments/disputes/resolve/{internalTransactionId:.+}", method = RequestMethod.POST)
    public ModelAndView resolve(@PathVariable("internalTransactionId") final String internalTransactionId,
                                @RequestParam("resolution") final DisputeResolution resolution,
                                @RequestParam("note") final String note) {
        notNull(internalTransactionId, "internalTransactionId may not be null");
        notNull(resolution, "resolution may not be null");

        paymentService.resolveDispute(internalTransactionId, resolution, authenticatedUser(), note);

        return this.openDisputes()
                .addObject("message", format("Dispute %s has been resolved with resolution %s", internalTransactionId, resolution));
    }

    private String authenticatedUser() {
        String authenticatingUser;
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticatingUser = "ANONYMOUS";
        } else {
            authenticatingUser = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        }
        return authenticatingUser;
    }

}
