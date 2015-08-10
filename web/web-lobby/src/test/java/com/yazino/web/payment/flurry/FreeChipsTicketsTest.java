package com.yazino.web.payment.flurry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FreeChipsTicketsTest {


    private FreeChipsTickets underTest;

    @Before
    public void setUp() throws Exception{
        underTest = new FreeChipsTickets();

    }

    @Test
    public void shouldGenerateNewTicket() {
        assertNotNull(underTest.newTicket());
    }

    @Test
    public void shouldReturnFalseForUnexpectedTicket() throws Exception {
        assertFalse(underTest.checkTicketAndRemove("foo",false));
    }

    @Test
    public void shouldReturnTrueForIssuedTicket()  throws Exception {
        String ticket = underTest.newTicket();
        assertTrue(underTest.checkTicketAndRemove(ticket, false));
    }

    @Test
    public void shouldReturnTrueForIssuedTicketOnceOnly()  throws Exception {
        String ticket = underTest.newTicket();
        underTest.checkTicketAndRemove(ticket, false);
        assertFalse(underTest.checkTicketAndRemove(ticket, false));
    }

    @Test
    public void shouldReturnTrueForEncryptedTicket()  throws Exception {
        String ticket = underTest.newTicket();
        String encrypted = new MobileEncryption().encryptTicket(ticket);
        assertTrue(underTest.checkTicketAndRemove(encrypted, true));
    }


}
