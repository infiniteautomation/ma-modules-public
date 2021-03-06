/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.model.publisher;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * @author Terry Packer
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXISTING_PROPERTY, property=AbstractPublishedPointModel.MODEL_TYPE)
public abstract class AbstractPublishedPointModel<T extends PublishedPointVO> {
    public static final String MODEL_TYPE = "modelType";

    protected String dataPointXid;
    
    public void fromVO(T vo) {
        this.dataPointXid = vo.getDataPointXid();
    }

    /**
     * Return the TYPE_NAME for the point's model
     * @return
     */
    public abstract String getModelType();
    
    /**
     * Create a vo from our fields
     * @return
     */
    public T toVO() {
        T vo = newVO();
        Integer id = DataPointDao.getInstance().getIdByXid(dataPointXid);
        if(id != null) {
            vo.setDataPointXid(dataPointXid);
            vo.setDataPointId(id);
        }else {
            //Only place we have the incomming XID to throw a validation exception
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("points", "validate.publisher.missingPointXid");
            throw new ValidationException(result);
        }
        return vo;
    }
    
    /**
     * Create an empty published point
     * @return
     */
    public abstract T newVO();
    
    public String getDataPointXid() {
        return dataPointXid;
    }
    
    public void setDataPointXid(String dataPointXid) {
        this.dataPointXid = dataPointXid;
    }
}
