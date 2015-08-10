package com.yazino.yaps;

import com.yazino.mobile.yaps.message.PushMessage;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TargetedMessageTransformerTest {

    private final TargetedMessageTransformer transformer = new TargetedMessageTransformer();

    @Test(expected = MessageTransformationException.class)
    public void shouldWrapAnyInvalidFieldExceptionsIntoAMessageTransformationException() throws Exception {
        TargetedMessage message = new TargetedMessage("1234", new PushMessage("TEST", BigDecimal.ONE));
        transformer.toBytes(message);
    }

    @Test
    public void shouldTransformOkMessageIntoBytes() throws Exception {
        TargetedMessage message = new TargetedMessage("354f4726a1da594ea216ff0dae37283041696b49b1cfdef66af283271d0c0888", new PushMessage("TEST", BigDecimal.ONE));
        byte[] bytes = transformer.toBytes(message);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

}
