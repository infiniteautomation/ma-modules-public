/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.model.event;

import com.infiniteautomation.mango.rest.latest.model.dataPoint.DataPointModel;
import com.infiniteautomation.mango.rest.latest.model.event.detectors.AbstractPointEventDetectorModel;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;

/**
 * @author Terry Packer
 *
 */

public class DataPointEventTypeModel extends AbstractEventTypeModel<DataPointEventType, DataPointModel, AbstractPointEventDetectorModel<?>> {

    private Integer dataSourceId;

    public DataPointEventTypeModel() {
        super(new DataPointEventType());
    }

    public DataPointEventTypeModel(DataPointEventType type) {
        super(type);
        this.dataSourceId = type.getDataSourceId();
    }

    public DataPointEventTypeModel(DataPointEventType type, DataPointModel reference1) {
        super(type, reference1);
        this.dataSourceId = type.getDataSourceId();
    }

    public DataPointEventTypeModel(DataPointEventType type, DataPointModel reference1, AbstractPointEventDetectorModel<?> reference2) {
        super(type, reference1, reference2);
        this.dataSourceId = type.getDataSourceId();
    }

    public Integer getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(Integer dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    @Override
    public DataPointEventType toVO() {
        return new DataPointEventType(dataSourceId, referenceId1, referenceId2, duplicateHandling);
    }


}
