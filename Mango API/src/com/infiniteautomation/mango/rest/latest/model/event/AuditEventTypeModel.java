/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.model.event;

import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;

/**
 * 
 * 
 * 
 * @author Terry Packer
 *
 */
public class AuditEventTypeModel extends AbstractEventTypeModel<AuditEventType, Object, Void> {

    private String changeType;
    
    public AuditEventTypeModel() {
        super(new AuditEventType());
    }
    
    public AuditEventTypeModel(AuditEventType type) {
        super(type);
        this.changeType = AuditEventInstanceVO.CHANGE_TYPE_CODES.getCode(type.getChangeType());
    }
    
    public String getChangeType() {
        return changeType;
    }

    @Override
    public AuditEventType toVO() {
        return new AuditEventType(subType, AuditEventInstanceVO.CHANGE_TYPE_CODES.getId(changeType), referenceId1);
    }
}
