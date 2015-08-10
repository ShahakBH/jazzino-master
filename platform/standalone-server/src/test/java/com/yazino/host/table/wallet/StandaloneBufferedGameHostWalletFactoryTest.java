package com.yazino.host.table.wallet;

import org.junit.Before;
import org.junit.Test;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWallet;

import java.math.BigDecimal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StandaloneBufferedGameHostWalletFactoryTest {

    private StandaloneBufferedGameHostWalletFactory underTest;

    @Before
    public void before() {
        underTest = new StandaloneBufferedGameHostWalletFactory(null, null);
    }

    @Test
    public void shouldCreateBufferedGameHostWallet() {
        final BufferedGameHostWallet hostWallet = underTest.create(BigDecimal.ONE);
        assertNotNull(hostWallet);
        assertTrue(hostWallet instanceof StandaloneBufferedGameHostWallet);
    }

    @Test
    public void shouldCreateBufferedGameHostWalletWithAuditLabel() {
        final BufferedGameHostWallet hostWallet = underTest.create(BigDecimal.ONE, "auditLabel");
        assertNotNull(hostWallet);
        assertTrue(hostWallet instanceof StandaloneBufferedGameHostWallet);
    }
}
