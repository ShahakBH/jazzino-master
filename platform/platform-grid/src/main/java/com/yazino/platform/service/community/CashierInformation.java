package com.yazino.platform.service.community;

import com.yazino.configuration.YazinoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CashierInformation {

    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public CashierInformation(YazinoConfiguration yazinoConfiguration) {
        this.yazinoConfiguration = yazinoConfiguration;
    }

    public boolean isPurchase(String cashierName) {
        final List<Object> list =
                yazinoConfiguration.getList("strata.cashier.purchase", Collections.emptyList());
        return list.contains(cashierName);
    }
}
