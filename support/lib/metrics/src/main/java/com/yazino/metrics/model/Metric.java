package com.yazino.metrics.model;

import org.joda.time.DateTime;

import java.io.Serializable;

public interface Metric<T extends Metric> extends Serializable {

    String getType();

    DateTime getTimestamp();

    T add(T metric);

}
