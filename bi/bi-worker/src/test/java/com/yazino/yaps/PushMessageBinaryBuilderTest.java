package com.yazino.yaps;

import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Date;

import static com.yazino.yaps.ConversionTools.*;
import static org.junit.Assert.assertEquals;

public class PushMessageBinaryBuilderTest {
    private static String SAMPLE_MESSAGE = "{\"aps\":{\"sound\":\"default\",\"alert\":\"Hello, world!\"}}";

    private final PushMessageBinaryBuilder builder = new PushMessageBinaryBuilder();

    @Test(expected = InvalidFieldException.class)
    public void shouldThrowExceptionWhenAttemptingToWriteIdentifierTooBig() throws Exception {
        builder.setIdentifier(new byte[5]);
    }

    @Test
    public void byteAtPositionZeroShouldBeCommandIdentifier() throws Exception {
        ByteBuffer buffer = buildAndCaptureBytes(0, 1);
        assertEquals(1, buffer.get());
    }

    @Test
    public void bytesBetweenOneAndFourShouldBePlayerId() throws Exception {
        builder.setIdentifier(intToByteArray(BigDecimal.ONE.intValue()));
        ByteBuffer buffer = buildAndCaptureBytes(1, 4);
        byte[] buf = buffer.array();
        assertEquals(1, bytesToInt(buf));
    }

    @Test
    public void bytesBetweenFiveAndEightShouldBeExpiryDate() throws Exception {
        int oneHourFromNow = (int) (new Date().getTime() + 3600) / 1000;
        builder.setExpiryDate(oneHourFromNow);
        ByteBuffer buffer = buildAndCaptureBytes(5, 4);
        assertEquals(oneHourFromNow, buffer.getInt());
    }

    @Test
    public void bytesBetweenNineAndTenShouldBeTokenLength() throws Exception {
        ByteBuffer buffer = buildAndCaptureBytes(9, 2);
        assertEquals(0, buffer.get(0));
        assertEquals(32, buffer.get(1));
    }

    @Test
    public void bytesBetweenElevenAndFortyThreeShouldBeDeviceToken() throws Exception {
        String deviceToken = "354f4726a1da594ea216ff0dae37283041696b49b1cfdef66af283271d0c0888";
        builder.setDeviceToken(deviceToken);
        ByteBuffer buffer = buildAndCaptureBytes(11, 32);
        String actual = byteArrayToHexString(buffer.array());
        assertEquals(deviceToken, actual);
    }

    @Test
    public void bytesBetweenElevenAndFortyThreeShouldBeDeviceToken_withSpaces() throws Exception {
        String deviceToken = "354f4726 a1da594e a216ff0d ae372830 41696b49 b1cfdef6 6af28327 1d0c0888";
        builder.setDeviceToken(deviceToken);
        ByteBuffer buffer = buildAndCaptureBytes(11, 32);
        String actual = byteArrayToHexString(buffer.array());
        assertEquals(deviceToken.replaceAll(" ", ""), actual);
    }

    @Test
    public void bytesBetweenElevenAndFortyThreeShouldBeDeviceToken_withBracketsAndSpaces() throws Exception {
        String deviceToken = "<354f4726 a1da594e a216ff0d ae372830 41696b49 b1cfdef6 6af28327 1d0c0888>";
        builder.setDeviceToken(deviceToken);
        ByteBuffer buffer = buildAndCaptureBytes(11, 32);
        String actual = byteArrayToHexString(buffer.array());
        assertEquals(deviceToken.replaceAll(" ", "").replaceAll("<", "").replaceAll(">", ""), actual);
    }

    @Test
    public void bytesBetweenFortyThreeAndFortyFourShouldBePayloadLength() throws Exception {
        builder.setPayload(SAMPLE_MESSAGE);
        ByteBuffer buffer = buildAndCaptureBytes(43, 2);
        assertEquals(0, buffer.get(0));
        assertEquals(SAMPLE_MESSAGE.replace("\\", "").length(), buffer.get(1));
    }

    @Test
    public void bytesBetweenFortyFiveAndEndShouldBePayload() throws Exception {
        builder.setPayload(SAMPLE_MESSAGE);
        String payload = SAMPLE_MESSAGE.replace("\\", "");
        int payloadLength = payload.length();
        ByteBuffer buffer = buildAndCaptureBytes(45, payloadLength);
        assertEquals(payload, new String(buffer.array()));
    }

    @Test
    public void bufferShouldBeResizedWhenPayloadChanges() throws Exception {
        assertEquals(PushMessageBinaryBuilder.MAX_LENGTH, builder.toBytes().length);
        builder.setPayload(SAMPLE_MESSAGE);
        String payload = SAMPLE_MESSAGE.replace("\\", "");
        int payloadLength = payload.length();
        int choppedOff = 256 - payloadLength;
        assertEquals(PushMessageBinaryBuilder.MAX_LENGTH - choppedOff, builder.toBytes().length);
    }

    private ByteBuffer buildAndCaptureBytes(int start, int length) throws IOException {
        byte[] bytes = builder.toBytes();
        byte[] subset = new byte[length];
        System.arraycopy(bytes, start, subset, 0, length);
        return ByteBuffer.wrap(subset);
    }


}
