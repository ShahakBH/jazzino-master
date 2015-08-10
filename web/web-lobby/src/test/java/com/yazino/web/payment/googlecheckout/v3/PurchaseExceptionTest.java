package com.yazino.web.payment.googlecheckout.v3;

import com.yazino.web.util.JsonHelper;
import org.junit.Test;

import static com.yazino.web.payment.PurchaseStatus.FAILED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;

public class PurchaseExceptionTest {

    @Test
    public void canSerializeAndDeserialize() {
        JsonHelper jsonHelper = new JsonHelper();
        PurchaseException expected = new PurchaseException(FAILED, true, "error message", "debug message", new RuntimeException("sample cause"));
        String serialized = jsonHelper.serialize(expected);
        System.out.println(serialized);
        PurchaseException deserialized = jsonHelper.deserialize(PurchaseException.class, serialized);
        assertThat(deserialized.getStatus(), equalTo(expected.getStatus()));
        assertThat(deserialized.canConsume(), equalTo(expected.canConsume()));
        assertThat(deserialized.getErrorMessage(), equalTo(expected.getErrorMessage()));
        assertThat(deserialized.getDebugMessage(), nullValue());
        assertThat(deserialized.getCause(), nullValue());
    }
}
