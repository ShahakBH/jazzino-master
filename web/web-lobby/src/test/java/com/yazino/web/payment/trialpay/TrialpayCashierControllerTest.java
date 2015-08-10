package com.yazino.web.payment.trialpay;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

import static com.yazino.platform.Platform.WEB;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TrialpayCashierControllerTest {
    private LobbySessionCache lobbySessionCache = Mockito.mock(LobbySessionCache.class);
    private TrialpayService trialpayService = Mockito.mock(TrialpayService.class);
    private TrialpayCashierController underTest;

    private HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    private String body;
    private String sid;
    private String transactionRef;
    private String rewardAmount;
    private String revenue;

    @Before
    public void setUp() throws Exception {

        underTest = new TrialpayCashierController(lobbySessionCache, "XYZ", "addVals", trialpayService);

        sid = "1940";
        transactionRef = "CW89CWY";
        rewardAmount = "1";
        revenue = "1";
        body = String.format("oid=%s&sid=%s&reward_amount=%s&revenue=%s", transactionRef, sid, rewardAmount, revenue);

    }


    @Test
    public void startShouldReturnPlayerIdAndCampaignId() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("scheme?");

        when(lobbySessionCache.getActiveSession(request)).thenReturn(new LobbySession(BigDecimal.valueOf(3141592), BigDecimal.ONE, "", "", Partner.YAZINO, "",
                "", null, false,
                WEB, AuthProvider.YAZINO));

        ModelAndView result = underTest.start(request);

        assertEquals("payment/trialpay/process", result.getViewName());
        assertEquals(BigDecimal.ONE, result.getModelMap().get("playerId"));
        assertEquals("XYZ", result.getModelMap().get("campaignId"));
        assertEquals("scheme?", result.getModelMap().get("scheme"));

    }


    @Test
    public void callbackShouldPassOnValuesToService() throws WalletServiceException, ServletException {
        when(request.getHeader("TrialPay-HMAC-MD5")).thenReturn("XYZ");
        underTest.callback(request,
                sid,
                rewardAmount,
                revenue,
                transactionRef
//                body
        );
        verify(trialpayService).payoutChipsAndNotifyPlayer(
                new BigDecimal(sid),
                new BigDecimal(rewardAmount),
                new BigDecimal(revenue),
                transactionRef,
                "xyz");
    }
}
