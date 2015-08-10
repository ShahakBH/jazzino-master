package com.yazino.web.domain;

import com.yazino.web.domain.PaymentEmailBodyTemplate;
import junit.framework.TestCase;


public class PaymentEmailBodyTemplateTest extends TestCase {
    public void testGetBody() throws Exception {

        assertEquals(PaymentEmailBodyTemplate.Zong.getBody("29034890832749823749832"), "using your mobile! ");
        assertEquals(PaymentEmailBodyTemplate.iTunes.getBody("29034890832749823749832"), "using iTunes. iTunes will send you a receipt of purchase shortly. ");
    }
}
