package com.yazino.platform.player.persistence;

import com.google.common.base.Optional;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.player.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PlayerProfileDao {

    void save(PlayerProfile playerProfile);

    PlayerProfile findByProviderNameAndExternalId(String providerName, String externalId);

    PlayerProfile findByPlayerId(BigDecimal playerId);

    Set<BigDecimal> findPlayerIdsByProviderNameAndExternalIds(Set<String> externalIds, String provider);

    PlayerProfile findByEmailAddress(String emailAddress);

    void updateStatus(BigDecimal playerId,
                      PlayerProfileStatus newStatus,
                      String changedBy,
                      String reason);

    void updateRole(BigDecimal playerId,
                    PlayerProfileRole newRole);

    List<PlayerProfileAudit> findAuditRecordsFor(BigDecimal playerId);

    int count();

    Map<String, BigDecimal> findRegisteredEmailAddresses(String... candidateEmailAddresses);

    Map<String, BigDecimal> findRegisteredExternalIds(String providerName, String... externalIds);

    void invalidateExternalId(String providerName, String externalId);

    PagedData<PlayerSearchResult> searchByEmailAddress(String emailAddress, int page, int pageSize);

    PagedData<PlayerSearchResult> searchByRealOrDisplayName(String name, int page, int pageSize);

    Optional<PlayerSummary> findSummaryById(BigDecimal playerId);

    Map<BigDecimal, String> findDisplayNamesByIds(Set<BigDecimal> playerIds);

    void invalidateGuestPlayer(String email);
}
