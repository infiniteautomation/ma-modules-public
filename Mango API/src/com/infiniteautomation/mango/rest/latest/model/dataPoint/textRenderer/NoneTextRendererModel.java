/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.model.dataPoint.textRenderer;

import com.serotonin.m2m2.view.text.NoneRenderer;

/**
 * @author Terry Packer
 *
 */
public class NoneTextRendererModel extends BaseTextRendererModel<NoneRenderer>{

    public NoneTextRendererModel() { }
    public NoneTextRendererModel(NoneRenderer vo) {
        fromVO(vo);
    }

    @Override
    public String getType() {
        return NoneRenderer.getDefinition().getName();
    }

    @Override
    NoneRenderer newVO() {
        return new NoneRenderer();
    }

    @Override
    public void fromVO(NoneRenderer vo) {

    }

    @Override
    public NoneRenderer toVO() {
        return newVO();
    }

}
