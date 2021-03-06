/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.model.pointValue.quantize;

import com.infiniteautomation.mango.quantize.AnalogStatisticsQuantizer;
import com.infiniteautomation.mango.quantize.BucketCalculator;
import com.infiniteautomation.mango.statistics.AnalogStatistics;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 *
 * @author Terry Packer
 */
public class AnalogStatisticsDataPointQuantizer extends DataPointStatisticsQuantizer<AnalogStatistics> {

    public AnalogStatisticsDataPointQuantizer(DataPointVO vo, BucketCalculator calc, ChildStatisticsGeneratorCallback callback) {
        super(vo, callback);
        quantizer = new AnalogStatisticsQuantizer(calc, this);
    }

}
