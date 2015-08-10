package com.yazino.platform.processor.tournament;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notEmpty;

@Service
public class PredefinedTableAllocatorFactory implements TableAllocatorFactory {
    private final Map<String, TableAllocator> idToAllocator = new HashMap<String, TableAllocator>();

    @Autowired
    public PredefinedTableAllocatorFactory(@Qualifier("tableAllocator") final Collection<TableAllocator> allocators) {
        notEmpty(allocators, "At least one table allocator must be provided");

        for (final TableAllocator allocator : allocators) {
            this.idToAllocator.put(allocator.getId(), allocator);
        }
    }

    public TableAllocator byId(final String id) {
        notBlank(id, "ID may not be null/blank");

        final TableAllocator tableAllocator = idToAllocator.get(id);
        if (tableAllocator == null) {
            throw new IllegalArgumentException("No matcher is available for ID: " + id);
        }

        return tableAllocator;
    }

}
