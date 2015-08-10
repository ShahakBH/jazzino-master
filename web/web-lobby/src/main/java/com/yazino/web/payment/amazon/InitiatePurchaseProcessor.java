package com.yazino.web.payment.amazon;

import com.yazino.platform.Platform;

import java.math.BigDecimal;

public interface InitiatePurchaseProcessor {

    Object initiatePurchase(BigDecimal playerId,
                              String productId,
                              Long promotionId,
                              String gameType,
                              Platform platform);

    Platform getPlatform();
}
