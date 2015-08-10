package com.yazino.bi.operations.engagement;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class GameTypeValidator implements Validator {

    private final ArrayList<String> validGameTypes;

    public GameTypeValidator(final ArrayList<String> validGameTypes) {
        this.validGameTypes = validGameTypes;
    }

    @Override
    public boolean supports(final Class<?> clazz) {
        return AppRequestTarget.class.equals(clazz);
    }

    @Override
    public void validate(final Object o, final Errors errors) {
        final Set<AppRequestTarget> targets = (Set<AppRequestTarget>) o;

        final Iterator<AppRequestTarget> targetsIter = targets.iterator();
        while (targetsIter.hasNext()) {
            final AppRequestTarget target = targetsIter.next();
            if (StringUtils.isBlank(target.getGameType()) || !validGameTypes.contains(target.getGameType())) {
                errors.rejectValue(null, "unknown", target.getGameType() + " is an unknown gameType for Id: "
                        + target.getId());
                targetsIter.remove();
            }
        }
    }
}
