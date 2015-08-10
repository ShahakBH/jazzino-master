package com.yazino.bi.operations.controller;

import com.yazino.bi.payment.persistence.JDBCPaymentChargebackDAO;
import com.yazino.configuration.YazinoConfiguration;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class ChargebackController {
    private static final int PAGE_SIZE = 20;
    private static final String PROPERTY_CHALLENGE_CODES = "payment.worldpay.chargeback.challenge";

    private final JDBCPaymentChargebackDAO chargebackDao;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public ChargebackController(final JDBCPaymentChargebackDAO chargebackDao,
                                final YazinoConfiguration yazinoConfiguration) {
        notNull(chargebackDao, "chargebackDao may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.chargebackDao = chargebackDao;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));
    }

    @RequestMapping(value = "/payments/chargebacks")
    public ModelAndView chargebacks(final ChargebackForm form) {
        return chargebacks(form, 1);
    }

    @RequestMapping(value = "/payments/chargebacks/{page}")
    public ModelAndView chargebacks(final ChargebackForm form,
                                    @PathVariable("page") final int page) {
        return new ModelAndView("payments/chargebacks")
                .addObject("form", form)
                .addObject("chargebacks", chargebackDao.search(asDateTime(form.getStartDate()), asDateTime(form.getEndDate()),
                        reasonCodesFor(form.isOnlyChallengeReasons()), page - 1, PAGE_SIZE));
    }

    @SuppressWarnings("unchecked")
    private List<String> reasonCodesFor(final boolean onlyChallengeReasons) {
        if (onlyChallengeReasons) {
            final String[] challengeCodes = yazinoConfiguration.getStringArray(PROPERTY_CHALLENGE_CODES);
            if (challengeCodes != null) {
                return asList(challengeCodes);
            }
        }
        return null;
    }

    private DateTime asDateTime(final Date date) {
        if (date != null) {
            return new DateTime(date);
        }
        return null;
    }

}
