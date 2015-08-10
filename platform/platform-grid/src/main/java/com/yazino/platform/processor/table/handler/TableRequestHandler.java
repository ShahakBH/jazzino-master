package com.yazino.platform.processor.table.handler;

import com.yazino.platform.model.table.TableRequest;
import com.yazino.platform.model.table.TableRequestType;

public interface TableRequestHandler<T extends TableRequest> {

    void handle(T request);

    boolean accepts(TableRequestType requestType);

}
