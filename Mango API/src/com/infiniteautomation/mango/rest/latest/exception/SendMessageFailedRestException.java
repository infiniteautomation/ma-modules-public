/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.rest.latest.exception;

import org.springframework.http.HttpStatus;

import com.infiniteautomation.mango.io.messaging.MessageSendException;

/**
 *
 * @author Terry Packer
 */
public class SendMessageFailedRestException extends AbstractRestException {

    private static final long serialVersionUID = 1L;

    public SendMessageFailedRestException(MessageSendException t) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, MangoRestErrorCode.GENERIC_500, t.getTranslatableMessage(), t);
    }
}
