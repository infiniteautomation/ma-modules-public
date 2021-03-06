/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest.websocket.dao;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.ApplicationEvent;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.rest.latest.websocket.DaoNotificationWebSocketHandler;
import com.infiniteautomation.mango.rest.latest.websocket.WebSocketMessageType;
import com.infiniteautomation.mango.rest.latest.websocket.WebSocketNotification;
import com.infiniteautomation.mango.rest.latest.websocket.WebSocketRequest;
import com.infiniteautomation.mango.rest.latest.websocket.WebSocketResponse;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Jared Wiltshire
 */
public abstract class SubscriptionDaoWebSocketHandler<T extends AbstractBasicVO> extends DaoNotificationWebSocketHandler<T> {

    public static final String SUBSCRIPTION_ATTRIBUTE = "DaoNotificationSubscription";
    public static final String REQUEST_TYPE_SUBSCRIPTION = "SUBSCRIPTION";

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="requestType")
    @JsonSubTypes({
        @JsonSubTypes.Type(name = REQUEST_TYPE_SUBSCRIPTION, value = DaoSubscription.class)
    })
    public static class DaoSubscriptionRequest extends WebSocketRequest {
    }

    public static class DaoSubscription extends DaoSubscriptionRequest {
        private Set<String> xids; // null means any xid
        private Set<String> notificationTypes; // null means any type

        public Set<String> getXids() {
            return xids;
        }
        public void setXids(Set<String> xids) {
            this.xids = xids;
        }
        public Set<String> getNotificationTypes() {
            return notificationTypes;
        }
        public void setNotificationTypes(Set<String> notificationTypes) {
            this.notificationTypes = notificationTypes;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode tree = this.jacksonMapper.readTree(message.getPayload());

        if (!WebSocketMessageType.REQUEST.messageTypeMatches(tree) || tree.get("requestType") == null) {
            return;
        }

        DaoSubscriptionRequest request = this.jacksonMapper.treeToValue(tree, DaoSubscriptionRequest.class);
        if (request instanceof DaoSubscription) {
            DaoSubscription subscription = (DaoSubscription) request;

            Set<String> notificationTypes = new HashSet<>();
            for (String type : subscription.getNotificationTypes()) {
                notificationTypes.add("add".equals(type) ? "create" : type);
            }
            subscription.setNotificationTypes(notificationTypes);

            session.getAttributes().put(SUBSCRIPTION_ATTRIBUTE, subscription);

            if (log.isDebugEnabled()) {
                log.debug("DAO subscription for " + session.getId() + " has been set to " + subscription);
            }

            this.sendRawMessage(session, new WebSocketResponse<Void>(subscription.getSequenceNumber()));
        }
    }

    @Override
    protected boolean isSubscribed(WebSocketSession session, String type, T vo, T originalVo) {
        DaoSubscription subscription = (DaoSubscription) session.getAttributes().get(SUBSCRIPTION_ATTRIBUTE);
        if (subscription == null) {
            return false;
        }

        Set<String> notificationTypes = subscription.getNotificationTypes();
        Set<String> xids = subscription.getXids();

        String xid;
        if (originalVo != null) {
            xid = this.getXid(originalVo);
        } else {
            xid = this.getXid(vo);
        }

        if (notificationTypes != null && !notificationTypes.contains(type)) {
            return false;
        }

        if (xids != null && !xids.contains(xid)) {
            return false;
        }

        return true;
    }

    @Override
    protected Object createNotification(String type, T vo, T originalVo, ApplicationEvent event, PermissionHolder user) {
        Object model = createModel(vo, event, user);
        if (model == null) {
            return null;
        }

        return new WebSocketNotification<Object>(type, model, Collections.singletonMap("originalXid", getXid(originalVo)));
    }

    protected String getXid(T vo) {
        if (vo instanceof AbstractVO) {
            return ((AbstractVO) vo).getXid();
        }
        return null;
    }
}
