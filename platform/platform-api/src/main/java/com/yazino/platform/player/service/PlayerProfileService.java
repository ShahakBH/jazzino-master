package com.yazino.platform.player.service;

import com.google.common.base.Optional;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.player.*;
import com.yazino.platform.worker.message.VerificationType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PlayerProfileService {

    PlayerProfile findByPlayerId(BigDecimal playerId);

    PlayerProfile findByProviderNameAndExternalId(String providerName,
                                                  String externalId);

    String findLoginEmailByPlayerId(BigDecimal playerId);

    /**
     * Convert a guest play account to a regular Yazino one.
     *
     * @param playerId     guest player to convert
     * @param emailAddress email address, assume to be a valid email address
     * @param password     player's new password
     * @param displayName  player's new display name
     * @return response indicating whether the conversion was successful or any errors
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if any of {code}emailAddress password displayName{code} are empty
     */
    PlayerProfileServiceResponse convertGuestToYazinoAccount(BigDecimal playerId, String emailAddress, String password, String displayName);

    /**
     * Convert a guest play account to a Facebook one.
     * delegates to the convertGuestToExternalAccount method
     *
     * @param playerId     guest player to convert
     * @param facebookId   player's FacebookId (i.e. external id)
     * @param displayName
     * @param emailAddress
     * @return response indicating whether the conversion was successful or any errors
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if any of {code}facebookId{code} are empty
     */
    PlayerProfileServiceResponse convertGuestToFacebookAccount(BigDecimal playerId, String facebookId, String displayName, String emailAddress);

    /**
     * Convert a guest play account to an External one.
     *
     * @param playerId     guest player to convert
     * @param externalId   player's ExternalId (i.e. external id)
     * @param displayName
     * @param emailAddress
     * @param partnerId    ID of partner. FACEBOOK,YAZINO,TANGO, etc
     * @return response indicating whether the conversion was successful or any errors
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if any of {code}externalId{code} are empty
     */
    PlayerProfileServiceResponse convertGuestToExternalAccount(BigDecimal playerId, String externalId, String displayName, String emailAddress, String partnerId);

    boolean updatePlayerInfo(BigDecimal playerId,
                             PlayerProfileSummary playerProfileSummary);

    boolean updateDisplayName(BigDecimal playerId,
                              String name);

    boolean updateEmailAddress(BigDecimal playerId,
                               String email);

    boolean updateAvatar(BigDecimal playerId,
                         Avatar avatar);

    void updatePassword(BigDecimal playerId,
                        PasswordChangeRequest passwordChangeRequest);

    void updateSyncFor(BigDecimal playerId,
                       boolean syncProfile);

    // TODO remove - think this method isn't used. If it is then should the updaters publish playerProfileEvent ?

    /**
     * @deprecated
     */
    PlayerProfileUpdateResponse update(PlayerProfile playerProfile,
                                       String password, final String avatarUrl);

    boolean verify(String email,
                   String verificationIdentifier,
                   VerificationType verificationType);

    void updateStatus(BigDecimal playerId,
                      PlayerProfileStatus status,
                      String changedBy,
                      String reason);

    void updateRole(BigDecimal playerId,
                    PlayerProfileRole role);

    List<PlayerProfileAudit> findAuditRecordsFor(BigDecimal playerId);

    ResetPasswordResponse resetPassword(String email);

    boolean exists(String emailAddress);

    int count();

    /**
     * Returns a set of registered email addresses from the candidate ones.
     *
     * @param candidateEmailAddresses the candidate email addresses
     * @return a Set of email addresses, or an empty set if none were found
     * @deprecated replaced by {@link #findByEmailAddresses(String... candidateEmailAddresses)}.
     */
    @Deprecated
    Set<String> findRegisteredEmailAddresses(String... candidateEmailAddresses);

    /**
     * Returns a set of registered external ids from the candidate ones.
     *
     * @param providerName         the provider of the external ids
     * @param candidateExternalIds the candidate email addresses
     * @return a Set of email addresses, or an empty set if none were found
     * @deprecated replaced by {@link #findByProviderNameAndExternalIds(String providerName, String... candidateExternalIds)}.
     */
    @Deprecated
    Set<String> findRegisteredExternalIds(String providerName, String... candidateExternalIds);

    /**
     * Find players that match the candidate email addresses.
     *
     * @param candidateEmailAddresses a bunch of email addresses to search for, no validation is performed on the input
     * @return a map of email addresses from the candidates that exist -> the player id
     */
    Map<String, BigDecimal> findByEmailAddresses(String... candidateEmailAddresses);

    /**
     * Find players that match the candidate provider and external ids.
     *
     * @param providerName         the provider, not null
     * @param candidateExternalIds a bunch of external ids to search for, no validation is performed on the input
     * @return a map of external ids from the candidates that exist -> the player id
     */
    Map<String, BigDecimal> findByProviderNameAndExternalIds(String providerName, String... candidateExternalIds);

    PagedData<PlayerSearchResult> searchByEmailAddress(String emailAddress, int page, int pageSize);

    PagedData<PlayerSearchResult> searchByRealOrDisplayName(String name, int page, int pageSize);

    Optional<PlayerSummary> findSummaryById(BigDecimal playerId);

    /**
     * This is a nasty method to cover the client not properly requesting these.
     * <p/>
     * Please do NOT use this for new work. Get the client to request this information on demand.
     *
     * @param playerIds the IDs to find display names for.
     * @return a map of IDs to display names.
     */
    Map<BigDecimal, String> findDisplayNamesById(Set<BigDecimal> playerIds);

}
