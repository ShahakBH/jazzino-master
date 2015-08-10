package com.yazino.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.yazino.web.util.RequestParameterUtils.hasParameter;

@Controller
public class PaymentResponseController {

    @RequestMapping(value = "/lobby/paymentResponse")
    public String processHelpRoulette(
            @RequestParam(value = "transactionId", required = false) final String transactionId,
            @RequestParam(value = "error", required = false) final String error,
            @RequestParam(value = "status", required = false) final String status,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final ModelMap model) {
        if (!hasParameter("status", status, request, response)) {
            return null;
        }

        if (transactionId != null) {
            model.addAttribute("transactionId", transactionId);
        }
        if (error != null) {
            model.addAttribute("error", error);
        }

        model.addAttribute("status", status);

        return "paymentResponse";
    }

}
