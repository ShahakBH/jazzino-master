package com.yazino.web.payment.paypalec;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller("paypalEcErrorController")
@RequestMapping("/payment/paypal-ec/error")
public class ErrorController {
    @RequestMapping(method = RequestMethod.GET)
    public void display() {

    }

}
