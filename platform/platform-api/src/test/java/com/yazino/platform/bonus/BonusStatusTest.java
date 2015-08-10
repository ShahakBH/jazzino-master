package com.yazino.platform.bonus;

import com.yazino.platform.JsonHelper;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class BonusStatusTest {

    @Test
    public void itShouldSerializeAndDeserialize() {

        final JsonHelper jsonHelper = new JsonHelper();
        final BonusStatus bonusStatus = new BonusStatus(DateTime.now().getMillis(), 1000l);
        final String serialized = jsonHelper.serialize(bonusStatus);
        System.out.println(serialized);
        final BonusStatus deserialized = jsonHelper.deserialize(BonusStatus.class, serialized);
        assertThat(bonusStatus, equalTo(deserialized));

    }
}
