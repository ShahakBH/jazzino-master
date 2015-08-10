package com.yazino.yaps;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class PushResponseTransformerTest {

    private final BigDecimal playerId = BigDecimal.TEN;

    private final PushResponseTransformer transformer = new PushResponseTransformer();

    @Test(expected = MessageTransformationException.class)
    public void shouldThrowExceptionIfMessageIsNotCorrectLength() throws Exception {
        byte[] bytes = new byte[7];
        transformer.fromBytes(bytes);
    }

    @Test
    public void shouldConvertResponseCode() throws Exception {
        byte[] bytes = new byte[6];
        bytes[0] = 8;
        bytes[1] = 5;
        System.arraycopy(ConversionTools.intToByteArray(playerId.intValue()), 0, bytes, 2, 4);

        PushResponse response = transformer.fromBytes(bytes);
        assertEquals(AppleResponseCode.InvalidTokenSize, response.getResponseCode());
    }

    @Test
    public void shouldConvertPlayerId() throws Exception {
        byte[] bytes = new byte[6];
        bytes[0] = 8;
        bytes[1] = 5;
        System.arraycopy(ConversionTools.intToByteArray(playerId.intValue()), 0, bytes, 2, 4);

        PushResponse response = transformer.fromBytes(bytes);
        assertEquals(playerId, response.getPlayerId());
    }


}
