package com.yazino.platform.gifting;

public enum Giftable {
    GIFTABLE(true), GIFTED(false);
    private final boolean giftable;

    Giftable(final boolean isGiftable) {
        giftable = isGiftable;
    }

    public boolean isGiftable() {
        return giftable;
    }
}
