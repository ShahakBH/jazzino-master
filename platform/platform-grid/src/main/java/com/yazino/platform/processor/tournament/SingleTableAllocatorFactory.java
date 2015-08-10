package com.yazino.platform.processor.tournament;

public class SingleTableAllocatorFactory implements TableAllocatorFactory {
    private final TableAllocator tableAllocator;

    public SingleTableAllocatorFactory(final TableAllocator tableAllocator) {
        this.tableAllocator = tableAllocator;
    }

    @Override
    public TableAllocator byId(final String id) {
        return tableAllocator;
    }
}
