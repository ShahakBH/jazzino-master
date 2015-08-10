package com.yazino.platform.processor.community;

import com.yazino.platform.community.RelationshipType;

public interface RelationshipActionProcessor {
    RelationshipType process(RelationshipType currentRelationship,
                             boolean processingInverseSide);
}
