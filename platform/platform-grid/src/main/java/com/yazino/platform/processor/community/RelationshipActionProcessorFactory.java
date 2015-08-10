package com.yazino.platform.processor.community;

import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.community.RelationshipType;

public final class RelationshipActionProcessorFactory {
    private static final UnblockProcessor UNBLOCK_PROCESSOR = new UnblockProcessor();
    private static final SetExternalFriendProcessor SET_EXTERNAL_FRIEND_PROCESSOR = new SetExternalFriendProcessor();
    private static final RemoveFriendProcessor REMOVE_FRIEND_PROCESSOR = new RemoveFriendProcessor();
    private static final RejectFriendProcessor REJECT_FRIEND_PROCESSOR = new RejectFriendProcessor();
    private static final PrivateChatProcessor PRIVATE_CHAT_PROCESSOR = new PrivateChatProcessor();
    private static final BlockProcessor BLOCK_PROCESSOR = new BlockProcessor();

    private RelationshipActionProcessorFactory() {
        // utility class
    }

    public static class UnblockProcessor implements RelationshipActionProcessor {
        public RelationshipType process(final RelationshipType currentRelationship,
                                        final boolean processingInverseSide) {
            if (!processingInverseSide) {
                if (currentRelationship == RelationshipType.IGNORED) {
                    return RelationshipType.NOT_FRIEND;
                }
                if (currentRelationship == RelationshipType.IGNORED_FRIEND) {
                    return RelationshipType.FRIEND;
                }
                return currentRelationship;
            } else {
                if (currentRelationship == RelationshipType.IGNORED_BY) {
                    return RelationshipType.NOT_FRIEND;
                }
                if (currentRelationship == RelationshipType.IGNORED_BY_FRIEND) {
                    return RelationshipType.FRIEND;
                }
                return currentRelationship;
            }
        }

    }

    public static class SetExternalFriendProcessor implements
            RelationshipActionProcessor {
        @Override
        public RelationshipType process(final RelationshipType currentRelationship,
                                        final boolean processingInverseSide) {
            if (currentRelationship == RelationshipType.NO_RELATIONSHIP) {
                return RelationshipType.FRIEND;
            }
            return currentRelationship;
        }
    }

    public static class RemoveFriendProcessor implements
            RelationshipActionProcessor {
        public RelationshipType process(final RelationshipType currentRelationship,
                                        final boolean processingInverseSide) {
            if (currentRelationship == RelationshipType.FRIEND) {
                return RelationshipType.NOT_FRIEND;
            }
            if (currentRelationship == RelationshipType.IGNORED_FRIEND) {
                return RelationshipType.IGNORED;
            }
            if (currentRelationship == RelationshipType.IGNORED_BY_FRIEND) {
                return RelationshipType.IGNORED_BY;
            }
            return currentRelationship;
        }

    }

    public static class RejectFriendProcessor implements
            RelationshipActionProcessor {
        public RelationshipType process(final RelationshipType currentRelationship,
                                        final boolean processingInverseSide) {
            if (!processingInverseSide && currentRelationship == RelationshipType.INVITATION_RECEIVED) {
                return RelationshipType.NOT_FRIEND;
            }
            if (processingInverseSide && currentRelationship == RelationshipType.INVITATION_SENT) {
                return RelationshipType.NOT_FRIEND;
            }
            return currentRelationship;
        }

    }

    public static class PrivateChatProcessor implements
            RelationshipActionProcessor {

        @Override
        public RelationshipType process(final RelationshipType currentRelationship,
                                        final boolean processingInverseSide) {
            throw new UnsupportedOperationException("not yet implemented");
        }

    }

    public static class BlockProcessor implements RelationshipActionProcessor {

        @Override
        public RelationshipType process(final RelationshipType currentRelationship,
                                        final boolean processingInverseSide) {
            if (!processingInverseSide) {
                if (currentRelationship == RelationshipType.FRIEND) {
                    return RelationshipType.IGNORED_FRIEND;
                }
                return RelationshipType.IGNORED;
            } else {
                if (currentRelationship == RelationshipType.FRIEND) {
                    return RelationshipType.IGNORED_BY_FRIEND;
                }
                return RelationshipType.IGNORED_BY;
            }
        }

    }

    public static class AddFriendProcessor implements
            RelationshipActionProcessor {
        public RelationshipType process(final RelationshipType currentRelationship,
                                        final boolean processingInverseSide) {
            if (!processingInverseSide) {
                if (currentRelationship == RelationshipType.NO_RELATIONSHIP
                        || currentRelationship == RelationshipType.NOT_FRIEND) {
                    return RelationshipType.INVITATION_SENT;
                }
                if (currentRelationship == RelationshipType.INVITATION_RECEIVED) {
                    return RelationshipType.FRIEND;
                }
            } else {
                if (currentRelationship == RelationshipType.NO_RELATIONSHIP
                        || currentRelationship == RelationshipType.NOT_FRIEND) {
                    return RelationshipType.INVITATION_RECEIVED;
                }
                if (currentRelationship == RelationshipType.INVITATION_SENT) {
                    return RelationshipType.FRIEND;
                }
            }
            return currentRelationship;
        }
    }

    public static class AcceptFriendProcessor implements RelationshipActionProcessor {

        public RelationshipType process(final RelationshipType currentRelationship,
                                        final boolean processingInverseSide) {
            if (!processingInverseSide) {
                if (currentRelationship == RelationshipType.INVITATION_RECEIVED) {
                    return RelationshipType.FRIEND;
                }
            } else {
                if (currentRelationship == RelationshipType.INVITATION_SENT) {
                    return RelationshipType.FRIEND;
                }
            }
            return currentRelationship;
        }

    }

    private static final RelationshipActionProcessor ACCEPT_FRIEND_PROCESSOR = new AcceptFriendProcessor();
    private static final AddFriendProcessor ADD_FRIEND_PROCESSOR = new AddFriendProcessor();

    public static RelationshipActionProcessor create(final RelationshipAction requestedAction) {
        switch (requestedAction) {
            case ACCEPT_FRIEND:
                return ACCEPT_FRIEND_PROCESSOR;
            case ADD_FRIEND:
                return ADD_FRIEND_PROCESSOR;
            case IGNORE:
                return BLOCK_PROCESSOR;
            case PRIVATE_CHAT:
                return PRIVATE_CHAT_PROCESSOR;
            case REJECT_FRIEND:
                return REJECT_FRIEND_PROCESSOR;
            case REMOVE_FRIEND:
                return REMOVE_FRIEND_PROCESSOR;
            case SET_EXTERNAL_FRIEND:
                return SET_EXTERNAL_FRIEND_PROCESSOR;
            case STOP_IGNORING:
                return UNBLOCK_PROCESSOR;
            default:
                throw new IllegalArgumentException("relationship action " + requestedAction + " not supported ");
        }
    }

}
