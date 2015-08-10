package com.yazino.platform.test;

import com.yazino.game.api.TransactionResult;

import java.util.List;

public interface PaymentsQueue {
    List<TransactionResult> pendingResults();

    void add(TransactionResult result);
}
