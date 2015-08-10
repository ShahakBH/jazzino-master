package com.yazino.platform.gamehost.preprocessing;


import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.game.api.Command;
import com.yazino.game.api.GameRules;

import java.math.BigDecimal;
import java.util.List;

public interface CommandPreprocessor {

    /**
     * Does some preprocessing of the specified command.
     *
     *
     * @param gameRules
     * @param command         the command, not null
     * @param playerBalance   the command owners balance
     * @param table           the table
     * @param auditLabel      the audit label
     * @param documentsToSend documents to send, any error documents should be added to this.
     * @return true if processing should continue
     */
    boolean preProcess(final GameRules gameRules, Command command,
                       BigDecimal playerBalance,
                       Table table,
                       String auditLabel,
                       List<HostDocument> documentsToSend);
}
