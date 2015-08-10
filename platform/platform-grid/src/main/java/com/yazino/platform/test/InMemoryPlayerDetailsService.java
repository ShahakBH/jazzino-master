package com.yazino.platform.test;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.*;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.player.GuestStatus;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryPlayerDetailsService implements PlayerService {

    @Autowired(required = false)
    private InMemoryInternalWalletService internalWalletService;

    @Autowired
    private InMemoryPlayerRepository playerRepository;

    private final AtomicInteger idSource = new AtomicInteger(-1);

    public void save(final BasicProfileInformation basicProfileInformation) {
        playerRepository.save(new Player(basicProfileInformation.getPlayerId(), basicProfileInformation.getName(),
                basicProfileInformation.getAccountId(), basicProfileInformation.getPictureUrl(), null, null, null));
    }


    public BasicProfileInformation findByName(final String name) {
        final Player player = playerRepository.findByName(name);
        if (player != null) {
            return getPlayer(player.getPlayerId());
        }
        return null;
    }

    public void setPlayerRepository(final InMemoryPlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public BasicProfileInformation createNewPlayer(final String displayName,
                                                   final String playerPictureUrl,
                                                   final GuestStatus guestStatus,
                                                   final PaymentPreferences paymentPreferences,
                                                   final PlayerCreditConfiguration playerCreditConfiguration) {
        final BigDecimal playerId = BigDecimal.valueOf(idSource.getAndIncrement());

        BigDecimal accountId = playerId;
        try {
            if (internalWalletService != null) {
                accountId = internalWalletService.createAccount("TestPlayerAccount:" + displayName);
            } else {
                System.err.println("Warning: Wallet Service is null, account is not present in DB");
            }
        } catch (WalletServiceException e) {
            e.printStackTrace();
        }

        final BasicProfileInformation basicProfileInformation = new BasicProfileInformation(
                playerId, displayName, playerPictureUrl, accountId);
        save(basicProfileInformation);

        return basicProfileInformation;
    }

    @Override
    public void updatePaymentPreferences(@Routing final BigDecimal playerId,
                                         final PaymentPreferences paymentPreferences) {
    }

    @Override
    public void registerFriends(@Routing final BigDecimal playerId,
                                final Set<BigDecimal> friendPlayerIds) {
    }

    @Override
    public void asyncRegisterFriends(@Routing final BigDecimal playerId,
                                     final Set<BigDecimal> friendPlayerIds) {

    }

    @Override
    public BigDecimal getAccountId(@Routing final BigDecimal playerId) {
        if (getPlayer(playerId) != null) {
            return getPlayer(playerId).getAccountId();
        }
        return null;
    }

    @Override
    public Set<BigDecimal> getFriends(final BigDecimal playerId) {
        return Collections.emptySet();
    }

    @Override
    public Map<BigDecimal, String> getFriendsOrderedByNickname(@Routing BigDecimal playerId) {
        return Collections.emptyMap();
    }

    @Override
    public List<BigDecimal> getFriendRequestsOrderedByNickname(@Routing BigDecimal playerId) {
        return Collections.emptyList();
    }

    @Override
    public PaymentPreferences getPaymentPreferences(@Routing final BigDecimal playerId) {
        return new PaymentPreferences();
    }

    private BasicProfileInformation getPlayer(final BigDecimal playerId) {
        final Player player = playerRepository.findById(playerId);
        if (player != null) {
            return new BasicProfileInformation(player.getPlayerId(), player.getName(), player.getPictureUrl(), player.getAccountId());
        }
        return null;
    }

    @Override
    public String getPictureUrl(final BigDecimal playerId) {
        return getPlayer(playerId).getPictureUrl();
    }

    @Override
    public Map<BigDecimal, Relationship> getRelationships(@Routing final BigDecimal playerId) {
        return Collections.emptyMap();
    }

    @Override
    public BigDecimal postTransaction(@Routing final BigDecimal playerId,
                                      final BigDecimal sessionId, final BigDecimal amountOfChips,
                                      final String transactionType,
                                      final String reference) throws WalletServiceException {
        return null;
    }

    @Override
    public ProfileInformation getProfileInformation(@Routing final BigDecimal playerId, final String gameType) {
        return null;
    }

    @Override
    public BasicProfileInformation getBasicProfileInformation(@Routing final BigDecimal playerId) {
        return getPlayer(playerId);
    }

    @Override
    public void publishFriendsSummary(@Routing final BigDecimal playerId) {
    }

    @Override
    public void asyncPublishFriendsSummary(@Routing final BigDecimal playerId) {
    }

    @Override
    public void addTag(@Routing final BigDecimal playerId, final String tag) {
        playerRepository.addTag(playerId, tag);
    }

    @Override
    public void removeTag(@Routing final BigDecimal playerId, final String tag) {
        playerRepository.removeTag(playerId, tag);
    }
}
