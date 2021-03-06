/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.model.event.detectors;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.latest.model.RestModelMapper;
import com.serotonin.m2m2.module.definitions.event.detectors.AlphanumericStateEventDetectorDefinition;
import com.serotonin.m2m2.vo.event.detector.AlphanumericStateDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Component
public class AlphanumericStateEventDetectorModelMapping extends AbstractPointEventDetectorModelMapping<AlphanumericStateDetectorVO, AlphanumericStateEventDetectorModel> {

    @Override
    public Class<? extends AlphanumericStateDetectorVO> fromClass() {
        return AlphanumericStateDetectorVO.class;
    }

    @Override
    public Class<? extends AlphanumericStateEventDetectorModel> toClass() {
        return AlphanumericStateEventDetectorModel.class;
    }

    @Override
    public AlphanumericStateEventDetectorModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        AlphanumericStateDetectorVO detector = (AlphanumericStateDetectorVO)from;
        return loadDataPoint(detector, new AlphanumericStateEventDetectorModel(detector), user, mapper);
    }
    
    @Override
    public String getTypeName() {
        return AlphanumericStateEventDetectorDefinition.TYPE_NAME;
    }
}
