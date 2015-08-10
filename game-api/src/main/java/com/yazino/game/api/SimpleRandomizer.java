package com.yazino.game.api;

import java.util.*;

public class SimpleRandomizer implements Randomizer {
    private Random random = new Random();

    void setRandom(final Random random) {
        this.random = random;
    }

    public void reset() {
        random = new Random();
    }

    public int nextInt() {
        return Math.abs(random.nextInt());
    }

    public int nextInt(final int n) {
        return Math.abs(random.nextInt(n));
    }

    @Override
    public int nextInt(final int minInclusive,
                       final int maxExclusive) {
        return nextInt(maxExclusive - minInclusive) + minInclusive;
    }

    public Set<Integer> getRandomNumbers(final int min,
                                         final int max,
                                         final int numberOfRandomsToGet) {
        final Set<Integer> out = new HashSet<Integer>();
        while (out.size() < numberOfRandomsToGet) {
            int nextRandom = nextInt(max * 2);
            nextRandom = nextRandom % (max + 1);
            if (nextRandom >= min) {
                out.add(nextRandom);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public Object getRandomItemFromCollection(final Collection collection) {
        final int index = nextInt() % collection.size();
        return new ArrayList(collection).get(index);

    }
}
