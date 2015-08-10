package com.yazino.web.controller;

import com.yazino.web.util.CookieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.yazino.web.util.RequestParameterUtils.hasParameter;

@Controller
public class TableReferralController {
    private static final Logger LOG = LoggerFactory.getLogger(TableReferralController.class);

    private final CookieHelper cookieHelper;
    private String hostUrl;

    @Autowired
    public TableReferralController(@Qualifier("cookieHelper") final CookieHelper cookieHelper) {
        this.cookieHelper = cookieHelper;
    }

    @Value("${senet.web.host}")
    public void setHostUrl(final String hostUrl) {
        this.hostUrl = hostUrl;
    }

    @RequestMapping({"/public/joinTable", "/joinTable"})
    public void refer(@RequestParam(value = "id", required = false) final String id,
                      final HttpServletRequest request,
                      final HttpServletResponse response) throws IOException {
        if (!hasParameter("id", id, request, response)) {
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("There's not active session. Storing cookie for table %s", id));
        }
        cookieHelper.setReferralTableId(response, id);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Redirecting to %s", hostUrl));
        }
        response.sendRedirect(hostUrl + "/lobby/games");
    }
}
