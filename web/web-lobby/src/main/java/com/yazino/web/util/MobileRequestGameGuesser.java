package com.yazino.web.util;

import com.yazino.platform.Platform;
import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class MobileRequestGameGuesser {

    public static final String SLOTS = "SLOTS";
    public static final String BLACKJACK = "BLACKJACK";
    public static final String HIGH_STAKES = "HIGH_STAKES";
    public static final String TEXAS_HOLDEM = "TEXAS_HOLDEM";

    public String guessGame(final HttpServletRequest request, final Platform platform) {
        final String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent == null) {
            return null;
        }
        switch (platform) {
            case AMAZON:
                return SLOTS;
            case ANDROID:
                return TEXAS_HOLDEM;
            case IOS:
                // only for legacy games... pre WD 3.0, BJ 2.0, HS 1.1
                if (userAgent.contains("Wheel Deal")) {
                    return SLOTS;
                }
                if (userAgent.contains("Blackjack")) {
                    return BLACKJACK;
                }
                if (userAgent.contains("High Stakes")) {
                    return HIGH_STAKES;
                }
            default:
                return null;
        }

    }
}
