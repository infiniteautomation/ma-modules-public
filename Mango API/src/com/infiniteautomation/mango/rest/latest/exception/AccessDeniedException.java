/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.rest.latest.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Jared Wiltshire
 */
public class AccessDeniedException extends AbstractRestException {
    private static final long serialVersionUID = 1L;

    public AccessDeniedException() {
        super(HttpStatus.FORBIDDEN, MangoRestErrorCode.ACCESS_DENIED);
    }

    public AccessDeniedException(Throwable cause) {
        super(HttpStatus.FORBIDDEN, MangoRestErrorCode.ACCESS_DENIED, cause);
    }

    public AccessDeniedException(TranslatableMessage message) {
        super(HttpStatus.FORBIDDEN, MangoRestErrorCode.ACCESS_DENIED, message);
    }

    public AccessDeniedException(TranslatableMessage message, Throwable cause) {
        super(HttpStatus.FORBIDDEN, MangoRestErrorCode.ACCESS_DENIED, message, cause);
    }
}
