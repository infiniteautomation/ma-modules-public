/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.model;

/**
 * Used to ensure Jackson is able to de-serialize the model (type T) in this mapping
 * 
 * @author Terry Packer
 *
 */
public interface RestModelJacksonMapping<F, T> extends RestModelMapping<F, T> {

    
    /**
     * Return the type name that maps the T class (Model) to a Type in Jackson
     */
    public String getTypeName();
    
}
