/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.websocket.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.latest.model.RestModelMapper;
import com.infiniteautomation.mango.rest.latest.model.mailingList.MailingListModelMapping;
import com.infiniteautomation.mango.rest.latest.websocket.DaoNotificationWebSocketHandler;
import com.infiniteautomation.mango.rest.latest.websocket.WebSocketMapping;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.service.MailingListService;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Jared Wiltshire
 */
@Component
@WebSocketMapping("/websocket/mailing-lists")
public class MailingListWebSocketHandler extends DaoNotificationWebSocketHandler<MailingList> {

    private final MailingListService service;
    private final MailingListModelMapping mapping;
    private final RestModelMapper mapper;

    @Autowired
    public MailingListWebSocketHandler(MailingListService service, MailingListModelMapping mapping,
            RestModelMapper mapper) {
        this.service = service;
        this.mapping = mapping;
        this.mapper = mapper;
    }

    @Override
    protected boolean hasPermission(PermissionHolder user, MailingList vo) {
        return service.hasReadPermission(user, vo);
    }

    @Override
    protected Object createModel(MailingList vo, ApplicationEvent event, PermissionHolder user) {
        return mapping.map(vo, user, mapper);
    }

    @Override
    @EventListener
    protected void handleDaoEvent(DaoEvent<? extends MailingList> event) {
        this.notify(event);
    }
}
