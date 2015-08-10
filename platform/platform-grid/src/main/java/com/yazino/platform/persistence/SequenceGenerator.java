package com.yazino.platform.persistence;

import java.math.BigDecimal;
import java.util.Set;

public interface SequenceGenerator {

    BigDecimal next();

    Set<BigDecimal> next(int numberOfKeys);

}
