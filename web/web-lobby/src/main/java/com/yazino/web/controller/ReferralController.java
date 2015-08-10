package com.yazino.web.controller;

import com.yazino.web.util.CookieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.yazino.web.util.RequestParameterUtils.hasParameter;

@Controller
public class ReferralController {
    private final CookieHelper cookieHelper;

    @Autowired(required = true)
    public ReferralController(@Qualifier("cookieHelper") final CookieHelper cookieHelper) {
        this.cookieHelper = cookieHelper;
    }

    @RequestMapping({ "/public/referral", "/referral" })
    public void refer(@RequestParam(value = "referral", required = false) final String referral,
            @RequestParam(value = "url", required = false) final String url,
            @RequestParam(value = "source", required = false) final String source, final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        if (!hasParameter("referral", referral, request, response) || !hasParameter("url", url, request, response)) {
            return;
        }

        cookieHelper.setReferralPlayerId(response, referral);
        cookieHelper.setScreenSource(response, source);
        response.sendRedirect(url);
    }
}
