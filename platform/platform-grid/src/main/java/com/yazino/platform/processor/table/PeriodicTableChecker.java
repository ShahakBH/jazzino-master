package com.yazino.platform.processor.table;

import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.model.table.AttemptToCloseTableRequest;
import com.yazino.platform.model.table.ProcessTableRequest;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.table.TableStatus;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PeriodicTableChecker implements InitializingBean, DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(PeriodicTableChecker.class);
    private static final Logger EVENT_LOG = LoggerFactory.getLogger("strata.debug.scheduled.events");
    private static final int ONE_HUNDRED_MS = 100;
    private ScheduledExecutorService executorService;
    // Delayed result bearing action
    private ScheduledFuture<?> sf;

    @GigaSpaceContext(name = "gigaSpace")
    private GigaSpace gigaSpace;

    private long defaultDelay = ONE_HUNDRED_MS;
    private static final String QUERY_STRING = "nextEventTimestamp <= ?";

    public PeriodicTableChecker() {
    }

    public long getDefaultDelay() {
        return defaultDelay;
    }

    public void setDefaultDelay(final long defaultDelay) {
        this.defaultDelay = defaultDelay;
    }

    public void setGigaSpace(final GigaSpace gigaSpace) {
        this.gigaSpace = gigaSpace;
    }


    @Override
    public void destroy() throws Exception {
        LOG.info("stopping periodic event checker");

        try {
            // Shutting down the thread pool upon bean disposal
            sf.cancel(true);
            sf = null;
            executorService.shutdown();

        } catch (Exception e) {
            LOG.error("Could not shutdown the event checker", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("initialising periodic event checker with delay of " + defaultDelay);
        }
        try {
            // Create a thread pool containing 1 thread capable of performing scheduled tasks
            executorService = Executors.newScheduledThreadPool(1);

            final EventTask eventTask = new EventTask();
            // Schedule the thread to execute the task at fixed rate with the default delay defined
            sf = executorService.scheduleAtFixedRate(eventTask, // The task to schedule
                    defaultDelay, // Initial Delay before starting
                    defaultDelay, // Delay between tasks
                    TimeUnit.MILLISECONDS // Time unit for the delay
            );

        } catch (Exception e) {
            LOG.error("Could not start periodic event checker", e);
        }
    }

    public class EventTask implements Runnable {
        public void run() {
            try {
                final SQLQuery<Table> query = new SQLQuery<Table>(Table.class, QUERY_STRING);
                query.setParameters(System.currentTimeMillis());
                final Table[] tables = gigaSpace.readMultiple(query, Integer.MAX_VALUE);
                for (Table table : tables) {
                    if (EVENT_LOG.isDebugEnabled()) {
                        EVENT_LOG.debug("Periodic event processing table id: " + table.getTableId());
                    }
                    final ProcessTableRequest processTableRequest = new ProcessTableRequest(table.getTableId());
                    gigaSpace.write(new TableRequestWrapper(processTableRequest));

                }
                final Table template = new Table();
                template.setTableStatus(TableStatus.closing);
                final Table[] closingTables = gigaSpace.readMultiple(template, Integer.MAX_VALUE);
                for (Table closingTable : closingTables) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Writing request to attempt closing table " + closingTable.getTableId());
                    }
                    gigaSpace.write(new TableRequestWrapper(new AttemptToCloseTableRequest(closingTable.getTableId())));
                }
            } catch (Exception e) {
                LOG.error("Task execution failed", e);
            }
        }
    }
}
