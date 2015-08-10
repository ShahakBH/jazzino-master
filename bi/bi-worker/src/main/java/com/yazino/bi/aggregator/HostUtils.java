package com.yazino.bi.aggregator;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class HostUtils {

    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown - host";
        }
    }

    //private constructor for checkstyle satisfaction
    private HostUtils() {
    }
}
