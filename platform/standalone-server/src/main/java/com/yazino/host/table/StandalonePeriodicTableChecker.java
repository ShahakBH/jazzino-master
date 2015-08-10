package com.yazino.host.table;

import com.yazino.host.TableRequestWrapperQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.model.table.ProcessTableRequest;

import java.math.BigDecimal;

@Component
public class StandalonePeriodicTableChecker implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(StandalonePeriodicTableChecker.class);
    private static final int CHECK_INTERVAL_IN_MILLIS = 500;
    private final TableRequestWrapperQueue requestWrapperQueue;

    @Autowired
    public StandalonePeriodicTableChecker(final TableRequestWrapperQueue requestWrapperQueue) {
        this.requestWrapperQueue = requestWrapperQueue;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    final ProcessTableRequest processTableRequest = new ProcessTableRequest(BigDecimal.ONE);
                    requestWrapperQueue.addRequest(new TableRequestWrapper(processTableRequest));
                    try {
                        Thread.sleep(CHECK_INTERVAL_IN_MILLIS);
                    } catch (InterruptedException e) {
                        LOG.error("Error in periodic table checker", e);
                    }
                }
            }
        }).start();
    }
}
