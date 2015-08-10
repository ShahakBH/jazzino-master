package com.yazino.platform.service.audit;

import com.yazino.platform.model.table.Table;
import com.yazino.game.api.Command;
import com.yazino.game.api.GameRules;

public interface Auditor extends AuditLabelFactory {

    void audit(String label, Command c);

    void audit(String label, Table t, GameRules gameRules);

}
