/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.latest.model.RestModelJacksonMapping;
import com.infiniteautomation.mango.rest.latest.model.RestModelMapper;
import com.infiniteautomation.mango.rest.latest.model.VirtualPointLocatorModel;
import com.serotonin.m2m2.virtual.vo.VirtualPointLocatorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;


/**
 *
 * @author Terry Packer
 *
 */
@Component
public class VirtualPointLocatorModelMapping implements RestModelJacksonMapping<VirtualPointLocatorVO, VirtualPointLocatorModel> {

    @Override
    public Class<? extends VirtualPointLocatorVO> fromClass() {
        return VirtualPointLocatorVO.class;
    }

    @Override
    public Class<? extends VirtualPointLocatorModel> toClass() {
        return VirtualPointLocatorModel.class;
    }

    @Override
    public VirtualPointLocatorModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        return new VirtualPointLocatorModel((VirtualPointLocatorVO)from);
    }

    @Override
    public String getTypeName() {
        return VirtualPointLocatorModel.TYPE_NAME;
    }
}
