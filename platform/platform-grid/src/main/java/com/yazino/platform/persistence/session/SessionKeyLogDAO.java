package com.yazino.platform.persistence.session;

import java.math.BigDecimal;

public interface SessionKeyLogDAO {
    void logSessionKey(BigDecimal accountId, String sessionKey, String ipAddress, String referer);
}
