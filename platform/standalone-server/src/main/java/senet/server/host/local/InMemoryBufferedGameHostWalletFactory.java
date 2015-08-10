package senet.server.host.local;


import com.yazino.platform.table.PlayerInformation;
import com.yazino.platform.test.InMemoryGameHostWallet;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.TransactionType;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWallet;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWalletFactory;
import com.yazino.platform.account.WalletServiceException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class InMemoryBufferedGameHostWalletFactory implements BufferedGameHostWalletFactory {

    private final InMemoryGameHostWallet gameHostWallet;

    @Autowired(required = true)
    public InMemoryBufferedGameHostWalletFactory(final InMemoryGameHostWallet gameHostWallet) {
        notNull(gameHostWallet, "gameHostWallet may not be null");

        this.gameHostWallet = gameHostWallet;
    }

    @Override
    public BufferedGameHostWallet create(final BigDecimal tableId) {
        return create(tableId, null);
    }

    @Override
    public BufferedGameHostWallet create(final BigDecimal tableId, final String auditLabel) {
        return new BufferedGameHostWallet() {
            private final List<Runnable> pendingTransactions = new ArrayList<Runnable>();

            @Override
            public void flush() {
                for (final Runnable tx : pendingTransactions) {
                    tx.run();
                }

                pendingTransactions.clear();
            }

            @Override
            public int numberOfPendingTransactions() {
                return pendingTransactions.size();
            }

            @Override
            public void post(final BigDecimal tableId,
                             final Long gameId,
                             final PlayerInformation playerInformation,
                             final BigDecimal amount,
                             final TransactionType transactionType,
                             final String auditLabel,
                             final String reference,
                             final String replyLabel)
                    throws WalletServiceException {
                final PlayerInformation immutablePlayerInfo = new PlayerInformation(
                        playerInformation.getPlayerId(), playerInformation.getName(),
                        playerInformation.getAccountId(), playerInformation.getSessionId(), playerInformation.getCachedBalance());
                pendingTransactions.add(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            gameHostWallet.post(tableId, gameId, immutablePlayerInfo,
                                    amount, transactionType, auditLabel, reference, replyLabel);

                        } catch (WalletServiceException e) {
                            System.err.println("Could not send pending tx");
                            e.printStackTrace();
                        }
                    }
                });

            }

            @Override
            public BigDecimal getBalance(final BigDecimal accountId)
                    throws WalletServiceException {
                return gameHostWallet.getBalance(accountId);
            }
        };
    }


}
