package com.yazino.platform.processor;

import java.io.Serializable;

public interface PersistenceRequest<T> extends Serializable {

    enum Status {
        PENDING,
        ERROR
    }

    int ROUTING_MODULUS = 20;

    T getObjectId();

    void setObjectId(T id);

    Status getStatus();

    void setStatus(Status status);

    Integer getSelector();

    void setSelector(Integer selector);

}
