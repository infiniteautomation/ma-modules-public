/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.model.event;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.latest.model.RestModelJacksonMapping;
import com.infiniteautomation.mango.rest.latest.model.RestModelMapper;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Component
public class AuditEventTypeModelMapping implements RestModelJacksonMapping<AuditEventType, AuditEventTypeModel> {

    @Override
    public Class<AuditEventType> fromClass() {
        return AuditEventType.class;
    }

    @Override
    public Class<AuditEventTypeModel> toClass() {
        return AuditEventTypeModel.class;
    }

    @Override
    public AuditEventTypeModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        return new AuditEventTypeModel((AuditEventType) from);
    }

    @Override
    public String getTypeName() {
        return EventType.EventTypeNames.AUDIT;
    }

}
