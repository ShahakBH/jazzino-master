package com.yazino.novomatic;

import com.yazino.novomatic.cgs.NovomaticCommand;

import java.io.Serializable;

public interface FakeEngineRules extends Serializable {
    FakeNovomaticState process(FakeNovomaticState gameState, NovomaticCommand command);
}
