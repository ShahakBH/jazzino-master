package com.yazino.web.session;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class ReferrerSessionCache {
    private static final Logger LOG = LoggerFactory.getLogger(ReferrerSessionCache.class);

    private final Set<String> facebookKnownRefs = new HashSet<String>();

    private String referrer = null;

    //cglib
    protected ReferrerSessionCache() {
    }

    @Autowired
    public ReferrerSessionCache(@Value("${facebook.knownRefs}") final String knownRefs) {
        notNull(knownRefs, "knownRefs is null");
        facebookKnownRefs.addAll(Arrays.asList(knownRefs.split(",")));
    }

    public void resolveReferrerFrom(final HttpServletRequest request) {
        if (referrer == null) {
            referrer = resolve(request);
        }
    }

    public void invalidate() {
        referrer = null;
    }

    private String resolve(final HttpServletRequest request) {
        final String ref = referenceParameterFrom(request);
        final boolean refPresent = !StringUtils.isBlank(ref);
        // "ref" can be overridden by Facebook. In this case we check if it's ours or theirs
        if (refPresent && isNotAFacebookRef(ref)) {
            return ref;
        }
        // "fb_ref" is provided by Facebook Open Graph links
        final String fbRef = request.getParameter("fb_ref");
        if (!StringUtils.isBlank(fbRef)) {
            return fbRef;
        }
        // "sourceId" is an alternative to "ref" (e.g. Facebook Wall Post links)
        final String sourceId = request.getParameter("sourceId");
        if (!StringUtils.isBlank(sourceId)) {
            return sourceId;
        }
        // "fb_source" is provided by Facebook internal links (e.g. Facebook Game Center)
        final String fbSource = request.getParameter("fb_source");
        if (!StringUtils.isBlank(fbSource)) {
            return fbSource;
        }
        // if the only information left is the Facebook one, use it
        if (refPresent) {
            return ref;
        }
        return null;
    }

    private String referenceParameterFrom(final HttpServletRequest request) {
        try {
            return request.getParameter("ref");

        } catch (IllegalArgumentException e) {
            LOG.warn("Unparsable ref received in URL: {}", request.getQueryString());
            return null;
        }
    }

    private boolean isNotAFacebookRef(final String ref) {
        return !facebookKnownRefs.contains(ref);
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(final String referrer) {
        this.referrer = referrer;
    }
}
