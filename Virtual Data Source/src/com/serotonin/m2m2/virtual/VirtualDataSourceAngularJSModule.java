/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.virtual;

import com.serotonin.m2m2.module.AngularJSModuleDefinition;

/**
 * @author Jared Wiltshire
 */
public class VirtualDataSourceAngularJSModule extends AngularJSModuleDefinition {
    @Override
    public String getJavaScriptFilename() {
        return "/angular/virtualDS.js";
    }
}
