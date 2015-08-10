package com.yazino.web.payment.radium;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class RadiumValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(RadiumValidationService.class);

    private final List<String> subnetAddresses;
    private final Map<String, String> appsAndKeys;


    public RadiumValidationService(final Map<String, String> appsAndKeys, final String subnetAddresses) {
        this.appsAndKeys = appsAndKeys;
        Validate.notNull(appsAndKeys);
        Validate.notNull(subnetAddresses);
        final String[] subnets = subnetAddresses.split(",");
        this.subnetAddresses = new ArrayList<String>();
        for (String subnet : subnets) {
            this.subnetAddresses.add(subnet);
        }
    }

    public boolean validate(final String userId, final String appId, final String hash) {
        if (isEmpty(userId) || isEmpty(appId) || isEmpty(hash)) {
            return false;
        }

        final String secretKey = appsAndKeys.get(appId);
        if (StringUtils.isBlank(secretKey)) {
            LOG.warn(String.format("validate being called for an app (%s) that is not configured with a secret key!",
                    appId));
        }
        final String concatenatedString = userId + ":" + appId + ":" + secretKey;
        try {
            final String hashedString = DigestUtils.md5DigestAsHex(concatenatedString.getBytes());

            final boolean hashMatches = hash.equals(hashedString);
            if (!hashMatches) {
                LOG.warn(String.format("hash doesn't match! concated string:%s+%s, hashcode:%s", userId, appId, hash));
            }
            return hashMatches;


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public boolean validateIp(final String ip) {
        for (String subnetAddress : subnetAddresses) {
            final SubnetUtils subnetUtils = new SubnetUtils(subnetAddress);
            subnetUtils.setInclusiveHostCount(true);
            if (subnetUtils.getInfo().isInRange(ip)) {
                return true;
            }
        }
        LOG.warn(String.format("validation failed on invalid ip %s!", ip));
        return false;

    }
}
