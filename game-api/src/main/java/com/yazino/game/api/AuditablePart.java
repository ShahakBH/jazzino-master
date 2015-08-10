package com.yazino.game.api;

public interface AuditablePart {
    void addAuditStringToBuffer(StringBuilder stringBuilder);

    void addAuditTitleStringToBuffer(StringBuilder builder);
}
