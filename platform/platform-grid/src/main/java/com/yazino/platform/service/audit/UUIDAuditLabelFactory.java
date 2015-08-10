package com.yazino.platform.service.audit;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("auditLabelFactory")
public class UUIDAuditLabelFactory implements AuditLabelFactory {

    @Override
    public String newLabel() {
        return UUID.randomUUID().toString();
    }
}
