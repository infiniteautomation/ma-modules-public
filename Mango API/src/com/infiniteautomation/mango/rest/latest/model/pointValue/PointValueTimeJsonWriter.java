/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.model.pointValue;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.infiniteautomation.mango.rest.latest.model.pointValue.query.LatestQueryInfo;

/**
 * @author Terry Packer
 *
 */
public class PointValueTimeJsonWriter extends PointValueTimeWriter {

    protected final JsonGenerator jgen;

    public PointValueTimeJsonWriter(LatestQueryInfo info, JsonGenerator jgen) {
        super(info);
        this.jgen = jgen;
    }

    @Override
    public void writeDataPointValue(DataPointValueTime value) throws IOException {
        this.jgen.writeStartObject();
        if(info.isMultiplePointsPerArray()) {
            this.jgen.writeObjectFieldStart(value.getVo().getXid());
            value.writeEntry(this, false, true);
            this.jgen.writeEndObject();
        }else {
            value.writeEntry(this, false, true);
        }
        this.jgen.writeEndObject();
    }

    /**
     * @param currentValues
     */
    @Override
    public void writeDataPointValues(List<DataPointValueTime> currentValues, long timestamp)  throws IOException{

        this.jgen.writeStartObject();
        //If we have a timestamp write it here
        if(info.fieldsContains(PointValueField.TIMESTAMP))
            writeTimestamp(timestamp);
        for(DataPointValueTime value : currentValues) {
            if(info.isMultiplePointsPerArray()) {
                this.jgen.writeObjectFieldStart(value.getVo().getXid());
                value.writeEntry(this, false, false);
                this.jgen.writeEndObject();
            }else {
                value.writeEntry(this, false, false);
            }
        }
        this.jgen.writeEndObject();
    }

    @Override
    public void writeStringField(String name, String value) throws IOException {
        this.jgen.writeStringField(name, value);
    }

    @Override
    public void writeDoubleField(String name, Double value) throws IOException {
        this.jgen.writeNumberField(name, value);
    }

    @Override
    public void writeIntegerField(String name, Integer value) throws IOException {
        this.jgen.writeNumberField(name, value);
    }

    @Override
    public void writeLongField(String name, Long value) throws IOException {
        this.jgen.writeNumberField(name, value);
    }

    @Override
    public void writeBooleanField(String name, Boolean value) throws IOException {
        this.jgen.writeBooleanField(name, value);
    }

    @Override
    public void writeNullField(String name) throws IOException {
        this.jgen.writeNullField(name);
    }

    @Override
    public void writeStartArray(String name) throws IOException {
        jgen.writeArrayFieldStart(name);
    }

    @Override
    public void writeEndArray() throws IOException {
        jgen.writeEndArray();
    }

    @Override
    public void writeStartArray() throws IOException {
        this.jgen.writeStartArray();
    }

    @Override
    public void writeStartObject(String name) throws IOException {
        this.jgen.writeObjectFieldStart(name);
    }

    @Override
    public void writeStartObject() throws IOException {
        this.jgen.writeStartObject();
    }

    @Override
    public void writeEndObject() throws IOException {
        this.jgen.writeEndObject();
    }
}
