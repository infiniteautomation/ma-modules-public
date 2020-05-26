/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.rest.v2.model.jsondata;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.rest.v2.model.AbstractVoModel;
import com.infiniteautomation.mango.rest.v2.model.permissions.MangoPermissionModel;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.vo.json.JsonDataVO;

/**
 *
 * @author Terry Packer
 */
public class JsonDataModel extends AbstractVoModel<JsonDataVO> {

    private MangoPermissionModel readPermission;
    private MangoPermissionModel editPermission;
    // TODO Mango 4.0 this is no longer needed?
    private boolean publicData;
    private JsonNode jsonData;

    public JsonDataModel(JsonDataVO data) {
        fromVO(data);
    }

    public JsonDataModel() { }

    public MangoPermissionModel getReadPermission() {
        return readPermission;
    }

    public void setReadPermission(MangoPermissionModel readPermission) {
        this.readPermission = readPermission;
    }

    public MangoPermissionModel getEditPermission() {
        return editPermission;
    }

    public void setEditPermission(MangoPermissionModel editPermission) {
        this.editPermission = editPermission;
    }

    public boolean isPublicData() {
        return publicData;
    }

    public void setPublicData(boolean publicData) {
        this.publicData = publicData;
    }

    public JsonNode getJsonData() {
        return jsonData;
    }

    public void setJsonData(JsonNode jsonData) {
        this.jsonData = jsonData;
    }

    @Override
    public void fromVO(JsonDataVO vo) {
        super.fromVO(vo);
        this.readPermission = new MangoPermissionModel(vo.getReadPermission());
        this.editPermission = new MangoPermissionModel(vo.getEditPermission());
        this.publicData = vo.isPublicData();
        this.jsonData = vo.getJsonData();
    }

    @Override
    public JsonDataVO toVO() throws ValidationException {
        JsonDataVO vo = super.toVO();
        vo.setReadPermission(readPermission != null ? readPermission.getPermission() : new MangoPermission());
        vo.setEditPermission(editPermission != null ? editPermission.getPermission() : new MangoPermission());
        vo.setPublicData(publicData);
        vo.setJsonData(jsonData);
        return vo;
    }

    @Override
    protected JsonDataVO newVO() {
        return new JsonDataVO();
    }

}
