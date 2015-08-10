package com.yazino.bi.aggregator;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class HostUtilsTest {

    @Test
    public void hostNameShouldReturnHostName() {
        Assert.assertFalse(StringUtils.isEmpty(HostUtils.getHostName()));
    }
}
