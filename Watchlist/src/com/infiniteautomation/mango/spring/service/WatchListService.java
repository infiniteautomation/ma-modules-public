/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.rest.latest.exception.ServerErrorException;
import com.infiniteautomation.mango.spring.dao.WatchListDao;
import com.infiniteautomation.mango.util.RQLUtils;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.rt.event.type.EventType.EventTypeNames;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.event.EventInstanceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.watchlist.WatchListCreatePermission;
import com.serotonin.m2m2.watchlist.WatchListVO;
import com.serotonin.m2m2.watchlist.WatchListVO.WatchListType;

import net.jazdw.rql.parser.ASTNode;

/**
 *
 * @author Terry Packer
 */
@Service
public class WatchListService extends AbstractVOService<WatchListVO, WatchListDao> {

    private final DataPointService dataPointService;
    private final EventInstanceService eventService;
    private final WatchListCreatePermission createPermission;
    private final DataPointDao dataPointDao;

    @Autowired
    public WatchListService(WatchListDao dao,
                            PermissionService permissionService,
                            DataPointService dataPointService,
                            EventInstanceService eventService,
                            WatchListCreatePermission createPermission, DataPointDao dataPointDao) {
        super(dao, permissionService);
        this.dataPointService = dataPointService;
        this.eventService = eventService;
        this.createPermission = createPermission;
        this.dataPointDao = dataPointDao;
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, WatchListVO vo) {
        return permissionService.hasPermission(user, vo.getEditPermission());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, WatchListVO vo) {
        return permissionService.hasPermission(user, vo.getReadPermission());
    }

    @Override
    protected PermissionDefinition getCreatePermission() {
        return createPermission;
    }

    @Override
    public ProcessResult validate(WatchListVO vo, PermissionHolder user) {
        ProcessResult response = commonValidation(vo, user);

        permissionService.validatePermission(response, "readPermission", user, vo.getReadPermission());
        permissionService.validatePermission(response, "editPermission", user, vo.getEditPermission());
        return response;

    }

    @Override
    public ProcessResult validate(WatchListVO existing, WatchListVO vo, PermissionHolder savingUser) {
        ProcessResult response = commonValidation(vo, savingUser);

        permissionService.validatePermission(response, "readPermission", savingUser, existing.getReadPermission(), vo.getReadPermission());
        permissionService.validatePermission(response, "editPermission", savingUser, existing.getEditPermission(), vo.getEditPermission());

        return response;
    }

    protected ProcessResult commonValidation(WatchListVO vo, PermissionHolder user) {
        ProcessResult response = super.validate(vo, user);
        if (vo.getType() == null) {
            String values = Arrays.asList(WatchListType.values()).toString();
            response.addContextualMessage("type", "validate.invalidValueWithAcceptable", vo.getType(), values);
        }

        if (vo.getType() == WatchListType.STATIC) {
            // Validate Points, we cannot trust the permissions from the passed in points from vo, we must look them up from the DB
            List<IDataPoint> newSummaries = vo.getPointList().stream().map(s -> {
                try {
                    return dataPointService.getSummary(s.getXid());
                } catch (PermissionException e) {
                    response.addContextualMessage("points", "watchlist.validate.pointNoReadPermission", s.getXid());
                } catch (NotFoundException e) {
                    response.addContextualMessage("points", "watchList.validate.pointNotFound", s.getXid());
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());

            vo.setPointList(newSummaries);
        }

        return response;
    }

    /**
     * Get the full data points for a list (TAG_TYPE not supported)
     * @param id
     * @param callback
     */
    public void getDataPoints(int id, Consumer<DataPointVO> callback) {
        WatchListVO vo = get(id);
        getDataPoints(vo, callback);
    }

    /**
     * Get the full data points for a list (TAG_TYPE not supported)
     * @param xid
     * @param callback
     */
    public void getDataPoints(String xid, Consumer<DataPointVO> callback) {
        WatchListVO vo = get(xid);
        getDataPoints(vo, callback);
    }

    /**
     * Get the full data points for a list (TAG_TYPE not supported)
     * @param vo
     * @param callback
     */
    public void getDataPoints(WatchListVO vo, Consumer<DataPointVO> callback) {
        PermissionHolder user = Common.getUser();

        switch(vo.getType()) {
            case STATIC:
                this.dao.getPoints(vo.getId(), (dp) -> {
                    if(dataPointService.hasReadPermission(user, dp)) {
                        callback.accept(dp);
                    }
                });
                break;
            case QUERY:
                if(vo.getParams().size() > 0)
                    throw new ServerErrorException(new TranslatableMessage("watchList.queryParametersNotSupported"));
                ASTNode rql = RQLUtils.parseRQLtoAST(vo.getQuery());
                ConditionSortLimit conditions = dataPointService.rqlToCondition(rql, null, null, null);
                dataPointService.customizedQuery(conditions, callback);
                break;
            case TAGS:
                throw new ServerErrorException(new TranslatableMessage("watchList.queryParametersNotSupported"));
            default:
                throw new ServerErrorException(new TranslatableMessage("common.default", "unknown watchlist type: " + vo.getType()));
        }
    }

    /**
     * Get data point events for a list
     * @param id
     * @param limit
     * @param offset
     * @param callback
     */
    public void getPointEvents(int id, Integer limit, Integer offset, Consumer<EventInstanceVO> callback) {
        WatchListVO vo = get(id);
        getPointEvents(vo, limit, offset, callback);
    }

    /**
     * Get data point events for a list
     * @param xid
     * @param limit
     * @param offset
     * @param callback
     */
    public void getPointEvents(String xid, Integer limit, Integer offset, Consumer<EventInstanceVO> callback) {
        WatchListVO vo = get(xid);
        getPointEvents(vo, limit, offset, callback);
    }

    /**
     *
     * @param vo
     * @param limit
     * @param offset
     * @param callback
     */
    public void getPointEvents(WatchListVO vo, Integer limit, Integer offset, Consumer<EventInstanceVO> callback) {
        PermissionHolder user = Common.getUser();
        List<Object> args = new ArrayList<>();
        args.add("typeRef1");

        switch(vo.getType()) {
            case STATIC:
                this.dao.getPoints(vo.getId(), (dp) -> {
                    if(dataPointService.hasReadPermission(user, dp)) {
                        args.add(Integer.toString(dp.getId()));
                    }
                });
                break;
            case QUERY:
                if(vo.getParams().size() > 0)
                    throw new ServerErrorException(new TranslatableMessage("watchList.queryParametersNotSupported"));
                ASTNode conditions = RQLUtils.parseRQLtoAST(vo.getQuery());
                dataPointService.customizedQuery(conditions, (dp) -> {
                    if(dataPointService.hasReadPermission(user, dp)) {
                        args.add(Integer.toString(dp.getId()));
                    }
                });
                break;
            case TAGS:
                throw new ServerErrorException(new TranslatableMessage("watchList.queryParametersNotSupported"));
            default:
                throw new ServerErrorException(new TranslatableMessage("common.default", "unknown watchlist type: " + vo.getType()));
        }
        //Create Event Query for these Points
        if(args.size() > 1) {
            ASTNode query = new ASTNode("in", args);
            if(user.getUser() != null) {
                query = addAndRestriction(query, new ASTNode("eq", "userId", user.getUser().getId()));
            }
            query = addAndRestriction(query, new ASTNode("eq", "typeName", EventTypeNames.DATA_POINT));

            if(limit != null) {
                if(offset == null) {
                    offset = 0;
                }
                query = addAndRestriction(query, new ASTNode("limit", limit, offset));
            }
            eventService.customizedQuery(query, (event) -> {
                if(eventService.hasReadPermission(user, event)) {
                    callback.accept(event);
                }
            });
        }
    }

    /**
     * Append an AND Restriction to a query
     * @param query - can be null
     * @param restriction
     * @return
     */
    protected static ASTNode addAndRestriction(ASTNode query, ASTNode restriction){
        //Root query node
        ASTNode root;

        if(query == null){
            root = restriction;
        }else if(query.getName().equalsIgnoreCase("and")){
            root = query.addArgument(restriction);
        }else{
            root = new ASTNode("and", restriction, query);
        }
        return root;
    }
}
