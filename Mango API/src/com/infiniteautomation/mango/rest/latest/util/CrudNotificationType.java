/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.util;

/**
 * @author Jared Wiltshire
 */
public enum CrudNotificationType {
    CREATE("create"), UPDATE("update"), DELETE("delete");
    
    private final String notificationType;
    
    private CrudNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getNotificationType() {
        return notificationType;
    }
}