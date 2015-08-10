package com.yazino.web.payment.creditcard;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class PurchaseOutcomeMapper {
    private final String defaultErrorMessage;
    private final Map<PurchaseOutcome, String> outcomeMapping = new HashMap<PurchaseOutcome, String>();

    @Autowired
    public PurchaseOutcomeMapper(@Qualifier("defaultErrorMessage") final String defaultErrorMessage,
                                 final Map<String, String> outcomeMapping) {
        notBlank(defaultErrorMessage, "defaultErrorMessage cannot be blank");
        notNull(outcomeMapping, "Mappings cannot be null");

        this.defaultErrorMessage = defaultErrorMessage;

        for (String outcomeAsString : outcomeMapping.keySet()) {
            this.outcomeMapping.put(PurchaseOutcome.valueOf(outcomeAsString),
                    outcomeMapping.get(outcomeAsString));
        }
    }

    public String getErrorMessage(final PurchaseOutcome outcome) {
        if (outcome == null) {
            return defaultErrorMessage;
        }

        final String errorMessage = outcomeMapping.get(outcome);
        if (errorMessage == null) {
            return defaultErrorMessage;
        }
        return errorMessage;
    }
}
