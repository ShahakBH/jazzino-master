package com.yazino.platform.grid;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A front end for GigaSpace remote execution.
 * <p/>
 * <code>ExecutorTestUtils</code> is supplied to assist in testing classes using this; it'll
 * catch a few edge cases that may surprise you.
 */
@Service
public class Executor {
    private static final int DEFAULT_TIMEOUT = 5000;

    private final GigaSpace gigaSpace;

    private int timeoutInMillis = DEFAULT_TIMEOUT;

    @Autowired
    public Executor(@Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace) {
        notNull(globalGigaSpace, "globalGigaSpace may not be null");

        this.gigaSpace = globalGigaSpace;
    }

    public <V extends Serializable, T> T mapReduce(final DistributedTask<V, T> task) {
        notNull(task, "task may not be null");

        try {
            return gigaSpace.execute(task).get(timeoutInMillis, TimeUnit.MILLISECONDS);

        } catch (ExecutionException e) {
            throw new ExecutorException(String.format("Remote execution failed for task %s", task), e.getCause());

        } catch (Exception e) {
            throw new ExecutorException(String.format("Remote execution caused an unexpected exception for task %s", task), e);
        }
    }

    public <V extends Serializable> V remoteExecute(final Task<V> task, final Object routingKey) {
        notNull(task, "task may not be null");
        notNull(routingKey, "routingKey may not be null");

        try {
            return gigaSpace.execute(task, routingKey).get(timeoutInMillis, TimeUnit.MILLISECONDS);

        } catch (ExecutionException e) {
            throw new ExecutorException(String.format("Remote execution failed for task %s with routing key %s",
                    task, routingKey), e.getCause());

        } catch (Exception e) {
            throw new ExecutorException(String.format("Remote execution caused an unexpected exception for task %s with routing key %s",
                    task, routingKey), e);
        }
    }

    public void setTimeoutInMillis(final int timeout) {
        this.timeoutInMillis = timeout;
    }
}
