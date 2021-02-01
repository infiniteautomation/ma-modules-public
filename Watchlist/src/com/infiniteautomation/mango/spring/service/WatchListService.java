/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.rest.latest.exception.ServerErrorException;
import com.infiniteautomation.mango.spring.dao.WatchListDao;
import com.infiniteautomation.mango.util.RQLUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.rt.event.type.EventType.EventTypeNames;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.event.EventInstanceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.watchlist.WatchListCreatePermission;
import com.serotonin.m2m2.watchlist.WatchListVO;
import com.serotonin.m2m2.watchlist.db.tables.WatchLists;
import com.serotonin.m2m2.watchlist.db.tables.records.WatchListsRecord;

import net.jazdw.rql.parser.ASTNode;

/**
 *
 * @author Terry Packer
 */
@Service
public class WatchListService extends AbstractVOService<WatchListVO, WatchListsRecord, WatchLists, WatchListDao> {

    private final DataPointService dataPointService;
    private final EventInstanceService eventService;
    private final WatchListCreatePermission createPermission;

    @Autowired
    public WatchListService(WatchListDao dao,
            PermissionService permissionService,
            DataPointService dataPointService,
            EventInstanceService eventService,
            WatchListCreatePermission createPermission) {
        super(dao, permissionService);
        this.dataPointService = dataPointService;
        this.eventService = eventService;
        this.createPermission = createPermission;
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

        permissionService.validatePermission(response, "readPermission", user, null, vo.getReadPermission());
        permissionService.validatePermission(response, "editPermission", user, null, vo.getEditPermission());
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
        switch(vo.getType()) {
            case WatchListVO.STATIC_TYPE:
            case WatchListVO.QUERY_TYPE:
            case WatchListVO.TAGS_TYPE:
                break;
            default:
                response.addContextualMessage("type", "validate.invalidValueWithAcceptable", vo.getType(), WatchListVO.STATIC_TYPE + "," + WatchListVO.QUERY_TYPE + "," + WatchListVO.TAGS_TYPE);
        }

        //Validate Points
        for(IDataPoint point : vo.getPointList()) {
            if(!permissionService.hasPermission(user, point.getReadPermission())) {
                response.addContextualMessage("points", "watchlist.validate.pointNoReadPermission", point.getXid());
            }
        }

        return response;
    }

    /**
     * Load data points from the mapping table for a watchlist
     */
    public void getWatchListPoints(int id, Consumer<DataPointVO> callback) {
        PermissionHolder user = Common.getUser();

        this.dao.getPoints(id, (dp) -> {
            if(dataPointService.hasReadPermission(user, dp)) {
                callback.accept(dp);
            }
        });
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
            case WatchListVO.STATIC_TYPE:
                this.dao.getPoints(vo.getId(), (dp) -> {
                    if(dataPointService.hasReadPermission(user, dp)) {
                        callback.accept(dp);
                    }
                });
                break;
            case WatchListVO.QUERY_TYPE:
                if(vo.getParams().size() > 0)
                    throw new ServerErrorException(new TranslatableMessage("watchList.queryParametersNotSupported"));
                ASTNode rql = RQLUtils.parseRQLtoAST(vo.getQuery());
                ConditionSortLimit conditions = dataPointService.rqlToCondition(rql, null, null, null);
                dataPointService.customizedQuery(conditions, (dp, index) -> callback.accept(dp));
                break;
            case WatchListVO.TAGS_TYPE:
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
            case WatchListVO.STATIC_TYPE:
                this.dao.getPoints(vo.getId(), (dp) -> {
                    if(dataPointService.hasReadPermission(user, dp)) {
                        args.add(Integer.toString(dp.getId()));
                    }
                });
                break;
            case WatchListVO.QUERY_TYPE:
                if(vo.getParams().size() > 0)
                    throw new ServerErrorException(new TranslatableMessage("watchList.queryParametersNotSupported"));
                ASTNode conditions = RQLUtils.parseRQLtoAST(vo.getQuery());
                dataPointService.customizedQuery(conditions, (dp, index) -> {
                    if(dataPointService.hasReadPermission(user, dp)) {
                        args.add(Integer.toString(dp.getId()));
                    }
                });
                break;
            case WatchListVO.TAGS_TYPE:
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
            eventService.customizedQuery(query, (event, index) -> {
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
