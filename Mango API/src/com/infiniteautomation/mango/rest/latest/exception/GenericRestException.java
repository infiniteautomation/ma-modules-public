/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 *
 * @author Terry Packer
 */
public class GenericRestException extends AbstractRestException {

    private static final long serialVersionUID = 1L;

    public GenericRestException(HttpStatus httpStatus) {
        super(httpStatus);
    }

    public GenericRestException(HttpStatus httpStatus, Throwable cause) {
        super(httpStatus, null, cause);
    }

    public GenericRestException(HttpStatus status, TranslatableMessage message) {
        super(status, null, message);
    }

    public GenericRestException(HttpStatus status, TranslatableMessage message, Throwable cause) {
        super(status, null, message, cause);
    }
}
