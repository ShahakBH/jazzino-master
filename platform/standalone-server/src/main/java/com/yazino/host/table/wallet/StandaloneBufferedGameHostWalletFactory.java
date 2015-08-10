package com.yazino.host.table.wallet;

import com.yazino.host.TableRequestWrapperQueue;
import com.yazino.platform.account.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWallet;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWalletFactory;

import java.math.BigDecimal;

@Component
public class StandaloneBufferedGameHostWalletFactory implements BufferedGameHostWalletFactory {
    private final WalletService walletService;
    private final TableRequestWrapperQueue tableRequestQueue;

    @Autowired
    public StandaloneBufferedGameHostWalletFactory(final WalletService walletService,
                                                   final TableRequestWrapperQueue tableRequestQueue) {
        this.walletService = walletService;
        this.tableRequestQueue = tableRequestQueue;
    }

    @Override
    public BufferedGameHostWallet create(final BigDecimal tableId) {
        return create(tableId, null);
    }

    @Override
    public BufferedGameHostWallet create(final BigDecimal tableId, final String auditLabel) {
        return new StandaloneBufferedGameHostWallet(walletService, tableRequestQueue);
    }


}
