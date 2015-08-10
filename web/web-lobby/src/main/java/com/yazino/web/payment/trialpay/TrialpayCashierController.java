package com.yazino.web.payment.trialpay;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;


@Controller("trialpayCashierController")
@RequestMapping("/payment/trialpay/") // NOTE! urlrewrite.xml is used to modify this from /strata.server.lobby.trialpay/
public class TrialpayCashierController {
    private final LobbySessionCache lobbySessionCache;
    private final String campaignId;
    private final String additionalParams;
    private final TrialpayService trialpayService;

    @Autowired
    public TrialpayCashierController(
            final LobbySessionCache lobbySessionCache,
            @Value("${strata.server.lobby.trialpay.campaignId}") final String campaignId,
            @Value("${strata.server.lobby.trialpay.additionalParams}") final String additionalParams,
            final TrialpayService trialpayService) {
        this.lobbySessionCache = lobbySessionCache;
        this.campaignId = campaignId;
        this.additionalParams = additionalParams;
        this.trialpayService = trialpayService;

    }

    @RequestMapping("/process")
    public ModelAndView start(final HttpServletRequest request) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        final ModelAndView mav = new ModelAndView("payment/trialpay/process");
        mav.addObject("playerId", lobbySession.getPlayerId());
        mav.addObject("additionalParams", additionalParams);
        mav.addObject("campaignId", campaignId);
        mav.addObject("scheme", request.getScheme());
        return mav;
    }

    @RequestMapping("/callback")
    public String callback(final HttpServletRequest request,
                           @RequestParam(value = "sid", required = false) final String sid,
                           @RequestParam(value = "reward_amount", required = false) final String rewardAmount,
                           @RequestParam(value = "revenue", required = false) final String rev,
                           @RequestParam(value = "oid", required = false) final String transactionRef
    )
            throws WalletServiceException, ServletException {

        final String expectedHash = request.getHeader("TrialPay-HMAC-MD5").toLowerCase();
        final BigDecimal playerId = new BigDecimal(sid);
        final BigDecimal amount = new BigDecimal(rewardAmount);
        final BigDecimal revenue = new BigDecimal(rev);
        return trialpayService.payoutChipsAndNotifyPlayer(playerId, amount, revenue, transactionRef, expectedHash);

    }
}
