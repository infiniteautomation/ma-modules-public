/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.rest.latest.model.event.detectors.rt;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.latest.model.RestModelMapper;
import com.infiniteautomation.mango.rest.latest.model.RestModelMapping;
import com.serotonin.m2m2.rt.event.detectors.BinaryStateDetectorRT;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Terry Packer
 */
@Component
public class BinaryStateEventDetectorRTModelMapping implements RestModelMapping<BinaryStateDetectorRT, BinaryStateEventDetectorRTModel> {

    @Override
    public Class<? extends BinaryStateDetectorRT> fromClass() {
        return BinaryStateDetectorRT.class;
    }

    @Override
    public Class<? extends BinaryStateEventDetectorRTModel> toClass() {
        return BinaryStateEventDetectorRTModel.class;
    }

    @Override
    public BinaryStateEventDetectorRTModel map(Object from, PermissionHolder user,
            RestModelMapper mapper) {
        return new BinaryStateEventDetectorRTModel((BinaryStateDetectorRT)from);
    }

}
