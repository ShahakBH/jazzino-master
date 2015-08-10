package com.yazino.web.payment.creditcard;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class IpAddressResolver {
    private static final Logger LOG = LoggerFactory.getLogger(IpAddressResolver.class);

    private IpAddressResolver() {

    }

    public static InetAddress resolveFor(final HttpServletRequest request) {
        final String clientIpAddressString = clientIpAddressFrom(request);

        LOG.debug("Client Address from Request: {}", clientIpAddressString);

        InetAddress clientAddress;
        try {
            clientAddress = Inet4Address.getByName(clientIpAddressString);
        } catch (UnknownHostException e) {
            LOG.error("Could not verify Client IP: {} for Credit Card Call ", clientIpAddressString, e);
            clientAddress = null;
        }

        LOG.debug("Client Address for Payment Request: {}", clientAddress);

        return clientAddress;
    }

    private static String clientIpAddressFrom(final HttpServletRequest request) {
        final String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress != null) {
            return StringUtils.substringBefore(ipAddress, ",");
        }
        return request.getRemoteAddr();
    }
}
