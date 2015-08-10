package com.yazino.host.table.game;

import com.yazino.model.VariationPropertiesSource;
import com.yazino.platform.table.GameVariation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.GameVariationRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

@Component
public class StandaloneGameVariationRepository implements GameVariationRepository {

    private final VariationPropertiesSource variationPropertiesSource;

    @Autowired
    public StandaloneGameVariationRepository(final VariationPropertiesSource variationPropertiesSource) {
        this.variationPropertiesSource = variationPropertiesSource;
    }

    @Override
    public void refreshAll() {
    }

    @Override
    public GameVariation findById(final BigDecimal id) {
        return new GameVariation(BigDecimal.ONE, "default", "default", Collections.<String, String>emptyMap());
    }

    @Override
    public BigDecimal getIdForName(final String name, final String gameType) {
        return BigDecimal.ONE;
    }

    public BigDecimal matchIdForName(final String name, final String gameType) {
        return BigDecimal.ONE;
    }

    @Override
    public void loadTemplatesIfRequired() {
    }

    @Override
    public void populateProperties(final Table table) {
        table.setVariationProperties(variationPropertiesSource.getVariationProperties());
    }

    @Override
    public Set<GameVariation> variationsFor(final String gameType) {
        return newHashSet(findById(BigDecimal.ONE));
    }
}
