/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.bulk;

/**
 * @author Jared Wiltshire
 */
public class VoIndividualRequest<B> extends IndividualRequest<VoAction, B> {
    private String xid;

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }
}
