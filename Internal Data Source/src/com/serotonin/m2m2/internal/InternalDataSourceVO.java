/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.dataSource.PollingDataSourceVO;
import com.serotonin.util.SerializationHelper;

/**
 * @author Matthew Lohbihler
 */
public class InternalDataSourceVO extends PollingDataSourceVO {

    @Override
    public int getPollAbortedExceptionEventId() {
        return InternalDataSourceRT.POLL_ABORTED_EVENT;
    }

    private static ExportCodes EVENT_CODES = new ExportCodes();
    static{
        EVENT_CODES.addElement(InternalDataSourceRT.POLL_ABORTED_EVENT, "POLL_ABORTED");
    }
    @Override
    public ExportCodes getEventCodes() {
        return EVENT_CODES;
    }

    @Override
    public TranslatableMessage getConnectionDescription() {
        return Common.getPeriodDescription(updatePeriodType, updatePeriods);
    }

    @Override
    public InternalDataSourceRT createDataSourceRT() {
        return new InternalDataSourceRT(this);
    }

    @Override
    public InternalPointLocatorVO createPointLocator() {
        return new InternalPointLocatorVO();
    }

    @JsonProperty
    private String createPointsPattern;

    public String getCreatePointsPattern() {
        return createPointsPattern;
    }

    public void setCreatePointsPattern(String createPointsPattern) {
        this.createPointsPattern = createPointsPattern;
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 3;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        SerializationHelper.writeSafeUTF(out, createPointsPattern);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            updatePeriodType = in.readInt();
            updatePeriods = in.readInt();
            createPointsPattern = null;
        }
        else if (ver == 2) {
            updatePeriodType = in.readInt();
            updatePeriods = in.readInt();
            createPointsPattern = SerializationHelper.readSafeUTF(in);
        }else if(ver == 3) {
            createPointsPattern = SerializationHelper.readSafeUTF(in);
        }
    }
}
