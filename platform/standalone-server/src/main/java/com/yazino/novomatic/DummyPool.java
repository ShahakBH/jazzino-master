package com.yazino.novomatic;

import com.yazino.novomatic.cgs.transport.ClientSocketConnection;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;

class DummyPool implements ObjectPool<ClientSocketConnection> {
    @Override
    public ClientSocketConnection borrowObject() throws Exception {
        return null;
    }

    @Override
    public void returnObject(ClientSocketConnection clientSocketConnection) throws Exception {
    }

    @Override
    public void invalidateObject(ClientSocketConnection clientSocketConnection) throws Exception {
    }

    @Override
    public void addObject() throws Exception {
    }

    @Override
    public int getNumIdle() throws UnsupportedOperationException {
        return 0;
    }

    @Override
    public int getNumActive() throws UnsupportedOperationException {
        return 0;
    }

    @Override
    public void clear() throws Exception {
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void setFactory(PoolableObjectFactory<ClientSocketConnection> clientSocketConnectionPoolableObjectFactory)
            throws IllegalStateException, UnsupportedOperationException {
    }
}
