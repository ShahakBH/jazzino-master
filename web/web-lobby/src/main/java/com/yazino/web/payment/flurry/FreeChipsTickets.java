package com.yazino.web.payment.flurry;


import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FreeChipsTickets {
    public static final int MAX_TICKETS = 300;
    private final List<String> issuedTickets;
    private final Object issuedTicketsLock = new Object();
    private final MobileEncryption crypto;

    public FreeChipsTickets() throws UnsupportedEncodingException {
        //This phrase is shared with IOS code
        issuedTickets = new ArrayList<String>();
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        crypto = new MobileEncryption();
    }

    public String newTicket() {
        final String newTicket = UUID.randomUUID().toString();
        synchronized (issuedTicketsLock) {
            while (issuedTickets.size() > MAX_TICKETS) {
                issuedTickets.remove(0);
            }
            issuedTickets.add(newTicket);
        }
        return newTicket;
    }

    public boolean checkTicketAndRemove(final String ticket,
                                        final boolean isEncrypted)
            throws IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, BadPaddingException, IOException {
        final String ticketRef = resolveTicketRef(ticket, isEncrypted);
        synchronized (issuedTicketsLock) {
            return issuedTickets.remove(ticketRef);
        }
    }

    private String resolveTicketRef(final String ticket, final boolean isEncrypted)
            throws IOException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (isEncrypted) {
            return crypto.decryptTicket(ticket);
        } else {
            return ticket;
        }
    }


}
