package com.yazino.test.game;

import com.yazino.game.api.SimpleRandomizer;

import java.util.LinkedList;
import java.util.Queue;

public class TestRandomizer extends SimpleRandomizer {
    private static final long serialVersionUID = 4090680630916426388L;
    private static final String DEFAULT_VALUE = "";
    private Queue<EnqueuedValue> nextInts = new LinkedList<EnqueuedValue>();

    public void reset() {
        super.reset();
        clear();
    }

    private Integer dequeue() {
        if (nextInts.size() == 0) {
            return null;
        }
        return nextInts.remove().value;
    }

    @Override
    public int nextInt() {
        final Integer next = dequeue();
        if (next == null) {
            return super.nextInt();
        }
        return next;
    }

    @Override
    public int nextInt(final int n) {
        final Integer next = dequeue();
        if (next == null) {
            return super.nextInt(n);
        }
        return next;
    }

    @Override
    public int nextInt(final int minInclusive,
                       final int maxExclusive) {
        Integer next = dequeue();
        if (next == null) {
            return super.nextInt(minInclusive, maxExclusive);
        }
        return next;
    }

    public void enqueueNext(final int... next) {
        for (int i : next) {
            nextInts.add(new EnqueuedValue(i, DEFAULT_VALUE));
        }
    }

    public void enqueueNext(final int value,
                            final String description) {
        nextInts.add(new EnqueuedValue(value, description));
    }

    public void clear() {
        nextInts.clear();
    }

    private final class EnqueuedValue {
        private int value;
        private String description;

        private EnqueuedValue(final int value, final String description) {
            this.value = value;
            this.description = description;
        }

        @Override
        public String toString() {
            return "EnqueuedValue{"
                    + "value=" + value
                    + ", description='" + description + '\''
                    + '}';
        }
    }
}
