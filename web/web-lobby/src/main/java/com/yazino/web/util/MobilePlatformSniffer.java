package com.yazino.web.util;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import static org.apache.http.HttpHeaders.USER_AGENT;

@Component
public class MobilePlatformSniffer {

    public enum MobilePlatform { ANDROID, IOS }

    public MobilePlatform inferPlatform(HttpServletRequest request) {
        String userAgent = StringUtils.trimToEmpty(request.getHeader(USER_AGENT)).toLowerCase();
        // Identifying IOS devices: http://stackoverflow.com/questions/4617638/detect-ipad-users-using-jquery/4617648#4617648
        if (userAgent.contains("iphone") || userAgent.contains("ipad") || userAgent.contains("ipod")) {
            return MobilePlatform.IOS;
        } else if (userAgent.contains("android")) {
            return MobilePlatform.ANDROID;
        }
        return null;
    }
}
