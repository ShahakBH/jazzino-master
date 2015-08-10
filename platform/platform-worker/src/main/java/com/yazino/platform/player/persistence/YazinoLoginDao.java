package com.yazino.platform.player.persistence;

import com.yazino.platform.player.YazinoLogin;

import java.math.BigDecimal;
import java.util.Map;

public interface YazinoLoginDao {

    YazinoLogin findByEmailAddress(String emailAddress);

    YazinoLogin findByPlayerId(BigDecimal playerId);

    void save(YazinoLogin login);

    boolean existsWithEmailAddress(String emailAddress);

    Map<String, BigDecimal> findRegisteredEmailAddresses(String... candidateEmailAddresses);

    void incrementLoginAttempts(String emailAddress);

    void resetLoginAttempts(String emailAddress);

    void deleteByPlayerId(BigDecimal playerId);
}
