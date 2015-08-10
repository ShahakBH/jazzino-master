package com.yazino.web.domain;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CashierConfigurationContainer {
    private final Map<String, CashierConfiguration> indexedConfigurations;

    public CashierConfigurationContainer(final List<CashierConfiguration> cashierConfigurations) {
        indexedConfigurations = new HashMap<String, CashierConfiguration>();
        for (CashierConfiguration config : cashierConfigurations) {
            indexedConfigurations.put(config.getCashierId(), config);
        }
    }

    public CashierConfiguration getCashierConfiguration(final String cashierId) {
        return indexedConfigurations.get(cashierId);
    }

    public boolean cashierExists(final String cashierId) {
        return indexedConfigurations.containsKey(cashierId);
    }
}
