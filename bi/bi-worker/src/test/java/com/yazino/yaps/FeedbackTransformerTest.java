package com.yazino.yaps;

import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.yazino.yaps.ConversionTools.dateToByteArray;
import static com.yazino.yaps.ConversionTools.hexStringToByteArray;
import static org.junit.Assert.assertEquals;

public class FeedbackTransformerTest {

    private static final String DEVICE_TOKEN = "354f4726a1da594ea216ff0dae37283041696b49b1cfdef66af283271d0c0888";
    private static final Date REMOVAL_DATE = createRemovalDate();

    private final FeedbackTransformer transformer = new FeedbackTransformer();
    private final byte[] message = new byte[38];

    @Before
    public void setup() throws ParseException {
        byte[] dateBytes = dateToByteArray(REMOVAL_DATE);
        assertEquals(4, dateBytes.length);
        System.arraycopy(dateBytes, 0, message, 0, 4);
        message[4] = 0;
        message[5] = 32;
        byte[] deviceToken = hexStringToByteArray(DEVICE_TOKEN);
        assertEquals(32, deviceToken.length);
        System.arraycopy(deviceToken, 0, message, 6, 32);
    }

    @Test(expected = MessageTransformationException.class)
    public void shouldThrowExceptionIfNotEnoughBytes() throws Exception {
        transformer.fromBytes(new byte[9]);
    }

    @Test(expected = MessageTransformationException.class)
    public void shouldThrowExceptionIfTooManyBytes() throws Exception {
        transformer.fromBytes(new byte[60]);
    }

    @Test
    public void shouldConvertTime() throws Exception {
        Feedback feedback = transformer.fromBytes(message);
        assertEquals(REMOVAL_DATE, feedback.getRemovalDate());
    }

    @Test
    public void shouldConvertDeviceToken() throws Exception {
        Feedback feedback = transformer.fromBytes(message);
        assertEquals(DEVICE_TOKEN, feedback.getDeviceToken());
    }

    @Test
    public void shouldGetCorrectDeviceTokenShouldSizeIncrease() {

    }

    private static Date createRemovalDate() {
        try {
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse("08/09/2011 09:34:55");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


}
