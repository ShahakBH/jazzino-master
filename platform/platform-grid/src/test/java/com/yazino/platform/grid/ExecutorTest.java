package com.yazino.platform.grid;

import com.gigaspaces.async.AsyncFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.Task;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExecutorTest {
    private static final String ROUTING_KEY = "aRoutingKey";

    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private DistributedTask<String, String> distributedTask;
    @Mock
    private Task<String> remoteExecutionTask;
    @Mock
    private AsyncFuture<String> distributedTaskResult;
    @Mock
    private AsyncFuture<String> remoteExecutionTaskResult;

    private Executor underTest;

    @Before
    public void setUp() throws InterruptedException, ExecutionException, TimeoutException {
        underTest = new Executor(gigaSpace);

        when(gigaSpace.execute(distributedTask)).thenReturn(distributedTaskResult);
        when(gigaSpace.execute(remoteExecutionTask, ROUTING_KEY)).thenReturn(remoteExecutionTaskResult);

        when(distributedTaskResult.get(5000, TimeUnit.MILLISECONDS)).thenReturn("distributedTaskResult");
        when(remoteExecutionTaskResult.get(5000, TimeUnit.MILLISECONDS)).thenReturn("remoteExecutionTaskResult");
    }

    @Test(expected = NullPointerException.class)
    public void anExecutorCannotBeCreatedWithANullGigaSpace() {
        new Executor(null);
    }

    @Test(expected = NullPointerException.class)
    public void anExecutorThrowsANullPointerExceptionForANullMapReduceTask() {
        underTest.mapReduce(null);
    }

    @Test
    public void anExecutorInvokesAMapReduceTaskOnTheGigaSpace() {
        underTest.mapReduce(distributedTask);

        verify(gigaSpace).execute(distributedTask);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = RuntimeException.class)
    public void anExecutorWrapsAnyExceptionFromAMapReduceInARuntimeException() throws InterruptedException, ExecutionException, TimeoutException {
        reset(distributedTaskResult);
        when(distributedTaskResult.get(5000, TimeUnit.MILLISECONDS))
                .thenThrow(new ExecutionException("Boo", new NullPointerException()));

        underTest.mapReduce(distributedTask);
    }

    @Test
    public void anExecutorPassesTheMapReduceTimeoutToTheResultGet() throws InterruptedException, ExecutionException, TimeoutException {
        underTest.setTimeoutInMillis(1234);

        underTest.mapReduce(distributedTask);

        verify(distributedTaskResult).get(1234, TimeUnit.MILLISECONDS);
    }

    @Test
    public void anExecutorReturnsTheResultOfAMapReduce() {
        final Object result = underTest.mapReduce(distributedTask);

        assertThat(result, is(equalTo((Object) "distributedTaskResult")));
    }

    @Test(expected = NullPointerException.class)
    public void anExecutorThrowsANullPointerExceptionForANullRemoteExecutionTask() {
        underTest.remoteExecute(null, ROUTING_KEY);
    }

    @Test(expected = NullPointerException.class)
    public void anExecutorThrowsANullPointerExceptionForANullRemoteExecutionRoutingKey() {
        underTest.remoteExecute(remoteExecutionTask, null);
    }

    @Test
    public void anExecutorInvokesARemoteExecutionTaskOnTheGigaSpace() {
        underTest.remoteExecute(remoteExecutionTask, ROUTING_KEY);

        verify(gigaSpace).execute(remoteExecutionTask, ROUTING_KEY);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = RuntimeException.class)
    public void anExecutorWrapsAnyExceptionFromARemoteExecutionInARuntimeException() throws InterruptedException, ExecutionException, TimeoutException {
        reset(remoteExecutionTaskResult);
        when(remoteExecutionTaskResult.get(5000, TimeUnit.MILLISECONDS))
                .thenThrow(new ExecutionException("Boo", new NullPointerException()));

        underTest.remoteExecute(remoteExecutionTask, ROUTING_KEY);
    }

    @Test
    public void anExecutorPassesTheRemoteExecutionTimeoutToTheResultGet() throws InterruptedException, ExecutionException, TimeoutException {
        underTest.setTimeoutInMillis(1234);

        underTest.remoteExecute(remoteExecutionTask, ROUTING_KEY);

        verify(remoteExecutionTaskResult).get(1234, TimeUnit.MILLISECONDS);
    }

    @Test
    public void anExecutorReturnsTheResultOfARemoteExecution() {
        final Object result = underTest.remoteExecute(remoteExecutionTask, ROUTING_KEY);

        assertThat(result, is(equalTo((Object) "remoteExecutionTaskResult")));
    }

}
