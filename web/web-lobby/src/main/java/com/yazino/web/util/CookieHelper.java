package com.yazino.web.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

@Service("cookieHelper")
public class CookieHelper {
    private static final Logger LOG = LoggerFactory.getLogger(CookieHelper.class);

    static final String REFERRAL_COOKIE_NAME = "REFERRALID";
    static final String TABLE_INVITATION_ID = "TABLEINVITATIONID";
    static final String LAST_GAME_TYPE_COOKIE_NAME = "LASTGAMETYPE";
    static final String PAYMENT_GAME_TYPE = "PAYMENT_GAME_TYPE";
    static final String SCREEN_SOURCE = "SCREEN_SOURCE";
    static final String ORIGINAL_GAME_TYPE = "ORIGINALGAMETYPE";
    static final String REDIRECT_TO = "REDIRECT_TO";

    private static final int ONE_WEEK_IN_SECONDS = 604800;
    private static final String ON_CANVAS = "CANVAS";


    public BigDecimal getReferralPlayerId(final Cookie[] cookies) {
        final String cookieValue = getCookieValue(cookies, REFERRAL_COOKIE_NAME, null);
        return parseBigDecimal(cookieValue);
    }

    public void setReferralPlayerId(final HttpServletResponse response, final String referralPlayerId) {
        setCookieValue(response, REFERRAL_COOKIE_NAME, referralPlayerId, true);
    }

    public String getRedirectTo(final HttpServletRequest request) {
        return getCookieValue(request.getCookies(), REDIRECT_TO, null);
    }

    public void setRedirectTo(final HttpServletResponse response, final String redirectTo) {
        setCookieValue(response, REDIRECT_TO, redirectTo, false);
    }

    public void setReferralTableId(final HttpServletResponse response, final String tableId) {
        if (!StringUtils.isBlank(tableId)) {
            setCookieValue(response, TABLE_INVITATION_ID, tableId, true);
        }
    }

    public BigDecimal getReferralTableId(final HttpServletRequest request, final HttpServletResponse response) {
        final String cookieValue = getCookieValue(request.getCookies(), TABLE_INVITATION_ID, null);
        if (!StringUtils.isBlank(cookieValue)) {
            final Cookie cookie = makeCookie(TABLE_INVITATION_ID, "", false);
            cookie.setMaxAge(0);
            response.addCookie(cookie);
            return parseBigDecimal(cookieValue);
        }
        return parseBigDecimal(cookieValue);
    }

    public boolean isOnCanvas(final HttpServletRequest request) {
        return isOnCanvas(request, null);
    }

    public boolean isOnCanvas(final HttpServletRequest request, final HttpServletResponse response) {
        final String valueFromRequest = request.getParameter("canvas");
        if (!StringUtils.isBlank(valueFromRequest)) {
            final boolean onCanvas = Boolean.TRUE.toString().equals(valueFromRequest);
            if (response != null) {
                setCookieValue(response, ON_CANVAS, String.valueOf(onCanvas), true);
            }
            return onCanvas;

        }
        return Boolean.valueOf(getCookieValue(request.getCookies(), ON_CANVAS, "false"));
    }

    public void setOriginalGameType(final HttpServletResponse response, final String gameType) {
        setCookieValue(response, ORIGINAL_GAME_TYPE, gameType, true);
    }

    public String getOriginalGameType(final HttpServletRequest request) {
        return getCookieValue(request.getCookies(), ORIGINAL_GAME_TYPE, null);
    }

    public void setLastGameType(final HttpServletResponse response, final String gameType) {
        setCookieValue(response, LAST_GAME_TYPE_COOKIE_NAME, gameType, true);
    }

    public String getLastGameType(final Cookie[] cookies, final String defaultGameType) {
        return getCookieValue(cookies, LAST_GAME_TYPE_COOKIE_NAME, defaultGameType);
    }

    Cookie makeCookie(final String cookieName, final String cookieValue, final boolean isLongLasting) {
        final Cookie cookie = new Cookie(cookieName, stripInvalidCookieValues(cookieValue));
        cookie.setPath("/");
        if (isLongLasting) {
            cookie.setMaxAge(ONE_WEEK_IN_SECONDS);
        }
        return cookie;
    }

    private String stripInvalidCookieValues(final String cookieValue) {
        if (cookieValue != null && cookieValue.contains(";")) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Removing excess data from cookie value of '%s'", cookieValue));
            }
            return cookieValue.substring(0, cookieValue.indexOf(";"));
        }
        return cookieValue;
    }

    void setCookieValue(final HttpServletResponse response, final String cookieName, final String cookieValue,
                        final boolean isLongLasting) {
        final Cookie cookie = makeCookie(cookieName, cookieValue, isLongLasting);
        response.addCookie(cookie);
    }

    String getCookieValue(final Cookie[] cookies, final String cookieName, final String defaultValue) {
        if (cookies == null) {
            return defaultValue;
        }
        for (final Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                if (StringUtils.isBlank(cookie.getValue())) {
                    return defaultValue;
                } else {
                    return cookie.getValue();
                }
            }
        }
        return defaultValue;
    }

    BigDecimal parseBigDecimal(final String cookieValue) {
        try {
            if (cookieValue == null) {
                return null;
            } else {
                return new BigDecimal(cookieValue);
            }
        } catch (final Exception e) {
            LOG.error("Error parsing BigDecimal " + cookieValue, e);
            return null;
        }
    }

    public void setOnCanvas(final HttpServletResponse response, final boolean onCanvas) {
        setCookieValue(response, ON_CANVAS, String.valueOf(onCanvas), false);
    }

    public void invalidateCanvas(final HttpServletResponse response) {
        Cookie cookieToDelete = new Cookie(ON_CANVAS, "false");
        cookieToDelete.setMaxAge(0);
        response.addCookie(cookieToDelete);
    }

    public String getPaymentGameType(final Cookie[] cookies) {
        return getCookieValue(cookies, PAYMENT_GAME_TYPE, null);
    }

    public void setPaymentGameType(final HttpServletResponse response, final String gameType) {
        if (gameType != null) {
            setCookieValue(response, PAYMENT_GAME_TYPE, gameType, false);
        }
    }

    public void setScreenSource(final HttpServletResponse response, final String source) {
        if (source != null) {
            setCookieValue(response, SCREEN_SOURCE, source, false);
        }
    }

    public String getScreenSource(final Cookie[] cookies) {
        return getCookieValue(cookies, SCREEN_SOURCE, null);
    }

    public void setIsNewPlayer(final HttpServletResponse response) {
        setCookieValue(response, "isNewPlayer", "true", false);
    }

}
