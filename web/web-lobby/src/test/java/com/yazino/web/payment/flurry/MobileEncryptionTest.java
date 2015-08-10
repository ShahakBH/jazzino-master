package com.yazino.web.payment.flurry;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MobileEncryptionTest {
    private static final String CLEAR = "some clear text to encode with legth";
    private static final String ENCRYPTED= "uEaT9CxT/EUwO0ZXY+0Etl/p+QqfTaOMsqJF4ui0VMOkwDjpiZhrAfQ5EBfoAC9N";

    private MobileEncryption underTest;

    @Before
    public void setUp() throws Exception{
        underTest = new MobileEncryption();
    }

    @Test
    public void shortText() throws Exception {
        String s = "some clear text";
        String encrypted = underTest.encryptTicket(s);
        assertEquals("vLTfFhjnDwSjGH/E/84i+A==",encrypted);
    }

    @Test
    public void shortTextNewLine() throws Exception {
        String s = "some clear text\nsome clear text";
        String encrypted = underTest.encryptTicket(s);
        assertEquals("RcGKxSv1YmQCWN8Ngqy3Cry03xYY5w8Eoxh/xP/OIvg=",encrypted);
    }

    @Test
    public void shouldEncrypt() throws Exception{
        String encrypted = underTest.encryptTicket(CLEAR);
        assertEquals(ENCRYPTED,encrypted);
    }


    @Test
    public void shouldEncryptTicketUUIDRoundTrip() throws Exception {
        String ticket = "97f27448-456a-46b0-839b-0f3d0fac7c83";
        String encrypted = underTest.encryptTicket(ticket);
        assertEquals("aV4u89TFJIDACe0p4/UagtPd4kzib6WLb1RDu8mRDG04jrZ5WMarSbEh19aAkWzc",encrypted);
        String decrypted = underTest.decryptTicket(encrypted);
        assertEquals(ticket,decrypted);

    }
    @Test
    public void shouldDecrypt() throws Exception {
        String decrypted = underTest.decryptTicket(ENCRYPTED);
        assertEquals(decrypted,CLEAR);
    }

    @Test
    public void shouldEncodeBase64() throws  Exception{
        String ticket = "97f27448-456a-46b0-839b-0f3d0fac7c83";
        byte[] encVal = ticket.getBytes("UTF8");
        String b64 = new String(Base64.encodeBase64(encVal));
        assertEquals("OTdmMjc0NDgtNDU2YS00NmIwLTgzOWItMGYzZDBmYWM3Yzgz",b64);
    }

}
