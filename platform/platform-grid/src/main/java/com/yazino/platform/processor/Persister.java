package com.yazino.platform.processor;

public interface Persister<T> {

    PersistenceRequest<T> persist(PersistenceRequest<T> persistenceRequest);

    Class<? extends PersistenceRequest<T>> getPersistenceRequestClass();

}
