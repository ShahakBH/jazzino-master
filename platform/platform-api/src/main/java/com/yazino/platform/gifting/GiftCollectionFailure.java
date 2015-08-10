package com.yazino.platform.gifting;

public class GiftCollectionFailure extends Throwable {
    private final CollectionResult collectionResult;

    public GiftCollectionFailure(final CollectionResult collectionResult) {
        super(collectionResult.name());
        this.collectionResult = collectionResult;
    }

    public CollectionResult getCollectionResult() {
        return collectionResult;
    }
}
