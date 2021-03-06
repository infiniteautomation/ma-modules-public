/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Useful when attempting to create something that already exists
 * 
 * @author Terry Packer
 */
public class AlreadyExistsRestException extends AbstractRestException {

    private static final long serialVersionUID = 1L;

    public AlreadyExistsRestException(String xid) {
        super(HttpStatus.CONFLICT, MangoRestErrorCode.ALREADY_EXISTS,
                new TranslatableMessage("rest.exception.alreadyExists", xid));
    }
}
