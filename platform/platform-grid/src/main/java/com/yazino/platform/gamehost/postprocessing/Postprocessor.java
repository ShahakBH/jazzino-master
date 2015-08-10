package com.yazino.platform.gamehost.postprocessing;


import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.game.api.Command;
import com.yazino.game.api.ExecutionResult;

import java.math.BigDecimal;
import java.util.List;

public interface Postprocessor {
    void postProcess(ExecutionResult executionResult,
                     Command command,
                     Table table,
                     String auditLabel,
                     List<HostDocument> documentsToSend,
                     BigDecimal initiatingPlayerId);
}
