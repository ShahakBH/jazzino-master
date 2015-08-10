package com.yazino.bi.persistence;

import java.util.List;

public interface BatchVisitor<T> {

    void processBatch(List<T> batch);

}
