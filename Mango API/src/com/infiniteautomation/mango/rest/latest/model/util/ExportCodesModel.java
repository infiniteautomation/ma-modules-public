/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.model.util;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.util.ExportCodes.Element;

/**
 * Model for export codes
 * @author Terry Packer
 */
public class ExportCodesModel {

    private List<ExportCodeElementModel> codes;
    public ExportCodesModel() { }
    
    public ExportCodesModel(ExportCodes codes) {
        this.codes = new ArrayList<>();
        for(Element e : codes.getElements())
            this.codes.add(new ExportCodeElementModel(e));
    }
    
    public List<ExportCodeElementModel> getCodes() {
        return codes;
    }

    public void setCodes(List<ExportCodeElementModel> codes) {
        this.codes = codes;
    }
}
