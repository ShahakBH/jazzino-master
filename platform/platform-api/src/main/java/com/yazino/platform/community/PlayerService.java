package com.yazino.platform.community;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.player.GuestStatus;
import org.openspaces.remoting.Routing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PlayerService {

    BasicProfileInformation createNewPlayer(String displayName,
                                            String playerPictureUrl,
                                            GuestStatus guestStatus,
                                            PaymentPreferences paymentPreferences,
                                            PlayerCreditConfiguration playerCreditConfiguration);

    void updatePaymentPreferences(@Routing BigDecimal playerId,
                                  PaymentPreferences paymentPreferences);

    void registerFriends(@Routing BigDecimal playerId,
                         Set<BigDecimal> friendPlayerIds);

    void asyncRegisterFriends(@Routing BigDecimal playerId,
                              Set<BigDecimal> friendPlayerIds);

    ProfileInformation getProfileInformation(@Routing BigDecimal playerId,
                                             String gameType);

    BasicProfileInformation getBasicProfileInformation(@Routing BigDecimal playerId);

    BigDecimal getAccountId(@Routing BigDecimal playerId);

    Set<BigDecimal> getFriends(@Routing BigDecimal playerId);

    /**
     * Returns an list of the player's friends, ordered by Nickname.
     *
     * @param playerId Player's ID
     * @return orderered list, may be empty, never null
     */
    Map<BigDecimal, String> getFriendsOrderedByNickname(@Routing BigDecimal playerId);

    /**
     * Returns an list of the player's friend requests, ordered by Nickname.
     *
     * @param playerId Player's ID
     * @return ordered list, may be empty, never null
     */
    List<BigDecimal> getFriendRequestsOrderedByNickname(@Routing BigDecimal playerId);

    PaymentPreferences getPaymentPreferences(@Routing BigDecimal playerId);

    String getPictureUrl(@Routing BigDecimal playerId);

    Map<BigDecimal, Relationship> getRelationships(@Routing BigDecimal playerId);

    /**
     * Allows posting a transaction which will result in adjusting player's balance.
     *
     * @param playerId        Player's ID
     * @param sessionId       the ID of the session requesting the post. May be null.
     * @param amountOfChips   Transaction amount (negative will remove from player's balance)
     * @param transactionType Transaction type
     * @param reference       Extra information to help tracking this transaction
     * @return Player's new balance
     * @throws WalletServiceException if couldn't process transaction (e.g. insufficient funds)
     */
    BigDecimal postTransaction(@Routing BigDecimal playerId,
                               BigDecimal sessionId,
                               BigDecimal amountOfChips,
                               String transactionType,
                               String reference)
            throws WalletServiceException;

    /**
     * Publish a friends summary for the given player.
     *
     * @param playerId the ID pf the player.
     */
    void publishFriendsSummary(@Routing BigDecimal playerId);

    void asyncPublishFriendsSummary(@Routing BigDecimal playerId);

    void addTag(@Routing BigDecimal playerId, final String tag);

    void removeTag(@Routing BigDecimal playerId, final String tag);

}
