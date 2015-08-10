package com.yazino.platform.grid;

import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.async.internal.DefaultAsyncResult;
import org.apache.commons.lang3.StringUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.Task;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ExecutorTestUtils {

    private ExecutorTestUtils() {

    }

    @SuppressWarnings("unchecked")
    public static Executor mockExecutorWith(final int partitionCount,
                                            final Map<String, Object> injectedServices,
                                            final Object... routingIds) {
        final Executor executor = mock(Executor.class);

        when(executor.mapReduce(any(DistributedTask.class))).then(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final DistributedTask task = (DistributedTask) invocation.getArguments()[0];
                inject(task, injectedServices);
                final List<AsyncResult> results = new ArrayList<AsyncResult>();
                for (int i = 0; i < partitionCount; ++i) {
                    try {
                        results.add(new DefaultAsyncResult(task.execute(), null));
                    } catch (Exception e) {
                        results.add(new DefaultAsyncResult(null, e));
                    }
                }
                return task.reduce(results);
            }
        });

        if (routingIds != null) {
            for (Object routingId : routingIds) {
                when(executor.remoteExecute(any(Task.class), eq(routingId))).then(new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        final Task task = (Task) invocation.getArguments()[0];
                        inject(task, injectedServices);
                        try {
                            return task.execute();
                        } catch (Exception e) {
                            throw new ExecutorException("anExecutorException", e);
                        }
                    }
                });
            }
        }

        return executor;
    }

    private static void inject(final Object target,
                               final Map<String, Object> injectedServices) {
        if (target.getClass().getAnnotation(AutowireTask.class) == null) {
            System.err.println(String.format("Class %s is not an AutowireTask, skipping injection", target.getClass().getName()));
            return;
        }

        for (Field currentField : target.getClass().getDeclaredFields()) {
            final Resource resourceAnnotation = currentField.getAnnotation(Resource.class);
            if (resourceAnnotation != null) {
                if (!Modifier.isTransient(currentField.getModifiers())) {
                    throw new IllegalStateException(String.format("Field %s of class %s is wired but not transient",
                            currentField.getName(), target.getClass().getName()));
                }

                Object injectionCandidate = null;
                if (!StringUtils.isBlank(resourceAnnotation.name())) {
                    if (injectedServices.containsKey(resourceAnnotation.name())) {
                        injectionCandidate = injectedServices.get(resourceAnnotation.name());
                    }
                } else {
                    final Class fieldType = currentField.getType();
                    for (Object injectedService : injectedServices.values()) {
                        if (fieldType.isAssignableFrom(injectedService.getClass())) {
                            injectionCandidate = injectedService;
                            break;
                        }
                    }
                }

                if (injectionCandidate != null) {
                    ReflectionTestUtils.setField(target, currentField.getName(), injectionCandidate);
                } else {
                    throw new IllegalStateException("Couldn't find injection candidate for field " + currentField.getName());
                }
            }
        }
    }

}
