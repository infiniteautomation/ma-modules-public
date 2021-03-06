/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.rest.latest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.latest.model.RestModelMapper;
import com.infiniteautomation.mango.rest.latest.model.WatchListModelMapping;
import com.infiniteautomation.mango.rest.latest.websocket.DaoNotificationWebSocketHandler;
import com.infiniteautomation.mango.rest.latest.websocket.WebSocketMapping;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.service.WatchListService;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.watchlist.WatchListVO;

/**
 *
 * @author Terry Packer
 */
@Component
@WebSocketMapping("/websocket/watch-lists")
public class WatchListWebsocketHandler extends DaoNotificationWebSocketHandler<WatchListVO> {

    private final WatchListService service;
    private final RestModelMapper mapper;
    private final WatchListModelMapping mapping;

    @Autowired
    public WatchListWebsocketHandler(WatchListService service,
            WatchListModelMapping mapping,
            RestModelMapper mapper) {
        this.service = service;
        this.mapping = mapping;
        this.mapper = mapper;
    }

    @Override
    protected boolean hasPermission(PermissionHolder user, WatchListVO vo) {
        return service.hasReadPermission(user, vo);
    }

    @Override
    protected Object createModel(WatchListVO vo, ApplicationEvent event, PermissionHolder user) {
        return mapping.map(vo, user, mapper);
    }

    @Override
    @EventListener
    protected void handleDaoEvent(DaoEvent<? extends WatchListVO> event) {
        this.notify(event);
    }

}
