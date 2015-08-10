package com.yazino.platform.gifting;

import com.yazino.platform.JsonHelper;
import com.yazino.platform.gifting.GiftableStatus;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GiftableStatusTest {
    private static final Logger LOG = LoggerFactory.getLogger(GiftableStatusTest.class);

    @Test
    public void shouldSerialiseAndDeserialise(){
        final GiftableStatus bob = new GiftableStatus(BigDecimal.ONE, Giftable.GIFTABLE, "your.mum", "BOB");
        final JsonHelper jsonHelper = new JsonHelper();
        String serialBob = jsonHelper.serialize(bob);
        LOG.info(serialBob);
        assertThat(jsonHelper.deserialize(GiftableStatus.class, serialBob), is(equalTo(bob)));
    }
}
