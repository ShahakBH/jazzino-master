package com.yazino.platform.gamehost.wallet;


import com.yazino.platform.account.GameHostWallet;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class SpaceBufferedGameHostWalletFactory implements BufferedGameHostWalletFactory {

    private final GameHostWallet gameHostWallet;
    private final GigaSpace tableGigaSpace;

    @Autowired(required = true)
    public SpaceBufferedGameHostWalletFactory(@Qualifier("gigaSpace") final GigaSpace tableGigaSpace,
                                              final GameHostWallet gameHostWallet) {
        notNull(tableGigaSpace, "tableGigaSpace may not be null");
        notNull(gameHostWallet, "gameHostWallet may not be null");

        this.tableGigaSpace = tableGigaSpace;
        this.gameHostWallet = gameHostWallet;
    }

    @Override
    public BufferedGameHostWallet create(final BigDecimal tableId) {
        return create(tableId, null);
    }

    @Override
    public BufferedGameHostWallet create(final BigDecimal tableId, final String auditLabel) {
        return new SpaceBufferedGameHostWallet(tableGigaSpace, gameHostWallet, tableId, auditLabel);
    }

}
