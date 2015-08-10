package com.yazino.web.interceptor;

import com.yazino.web.session.ReferrerSessionCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class RefererInterceptor extends HandlerInterceptorAdapter {
    private final List<String> blacklistedUrls = new ArrayList<String>();
    private final ReferrerSessionCache referrerSessionCache;

    @Autowired
    public RefererInterceptor(final ReferrerSessionCache referrerSessionCache,
                              final List<String> blacklistedUrls) {
        notNull(referrerSessionCache, "referrerSessionCache may not be null");
        notNull(blacklistedUrls, "blacklistedUrls may not be null");

        this.referrerSessionCache = referrerSessionCache;
        this.blacklistedUrls.addAll(blacklistedUrls);
    }

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler)
            throws Exception {
        if (isNotBlacklisted(request.getRequestURI())) {
            referrerSessionCache.resolveReferrerFrom(request);
        }
        return super.preHandle(request, response, handler);
    }

    private boolean isNotBlacklisted(final String requestUrl) {
        for (String blacklistedUrl : blacklistedUrls) {
            if (requestUrl.startsWith(blacklistedUrl)) {
                return false;
            }
        }
        return true;
    }
}
