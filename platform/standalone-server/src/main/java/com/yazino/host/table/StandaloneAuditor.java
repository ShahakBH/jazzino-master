package com.yazino.host.table;

import org.springframework.stereotype.Component;
import com.yazino.game.api.Command;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.service.audit.Auditor;
import com.yazino.game.api.GameRules;

@Component
public class StandaloneAuditor implements Auditor {
    private int labelCounter = 0;

    @Override
    public void audit(final String label, final Command c) {
    }

    @Override
    public void audit(final String label, final Table t, final GameRules gameRules) {
    }

    @Override
    public String newLabel() {
        labelCounter++;
        return "audit-" + labelCounter;
    }
}
