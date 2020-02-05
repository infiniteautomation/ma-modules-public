/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.model;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jooq.Field;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonGenerator;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.rest.v2.exception.GenericRestException;
import com.infiniteautomation.mango.spring.db.AbstractBasicTableDefinition;
import com.infiniteautomation.mango.spring.service.AbstractBasicVOService;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;

import net.jazdw.rql.parser.ASTNode;

/**
 * @author Jared Wiltshire
 * @author Terry Packer
 */
public class StreamedBasicVORqlQueryWithTotal<T extends AbstractBasicVO, TABLE extends AbstractBasicTableDefinition, DAO extends AbstractBasicDao<T, TABLE>, SERVICE extends AbstractBasicVOService<T, TABLE, DAO>> implements StreamedArrayWithTotal {

    protected final SERVICE service;
    protected final ConditionSortLimit conditions;
    protected final Function<T, ?> toModel;
    protected final Predicate<T> filter;
    protected int count;

    /**
     * Use if permissions cannot be enforced in the RQL/Database query, this will perform a full query and count the results while respecting the limit.
     * @param service
     * @param rql
     * @param fieldMap
     * @param valueConverterMap
     * @param filter
     * @param toModel
     */
    public StreamedBasicVORqlQueryWithTotal(SERVICE service, ASTNode rql, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverterMap, Predicate<T> filter, Function<T, ?> toModel) {
        this(service, service.rqlToCondition(rql, fieldMap, valueConverterMap), filter, toModel);
    }

    /**
     * Variant to use if the permissions can be enforced via the RQL/Database query
     * @param service
     * @param rql
     * @param fieldMap
     * @param valueConverterMap
     * @param toModel
     */
    public StreamedBasicVORqlQueryWithTotal(SERVICE service, ASTNode rql, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverterMap, Function<T, ?> toModel) {
        this(service, service.rqlToCondition(rql, fieldMap, valueConverterMap), null, toModel);
    }

    /**
     * Variant to use if permissions can be enforced via the RQL/Database query
     * @param service
     * @param conditions
     * @param toModel
     */
    public StreamedBasicVORqlQueryWithTotal(SERVICE service, ConditionSortLimit conditions, Function<T, ?> toModel) {
        this(service, conditions, null, toModel);
    }

    /**
     * Use if permissions cannot be enforced in the RQL/Database query, this will perform a full query and count the results while respecting the limit.
     * @param service
     * @param conditions
     * @param filter
     * @param toModel
     */
    public StreamedBasicVORqlQueryWithTotal(SERVICE service, ConditionSortLimit conditions, Predicate<T> filter, Function<T, ?> toModel) {
        this.service = service;
        this.conditions = conditions;
        this.toModel = toModel;
        this.filter = filter;
    }

    @Override
    public StreamedArray getItems() {
        return new StreamedVOArray();
    }

    @Override
    public int getTotal() {
        if(filter != null) {
            return count;
        }else {
            return service.customizedCount(conditions);
        }
    }

    private class StreamedVOArray implements JSONStreamedArray {
        @Override
        public void writeArrayValues(JsonGenerator jgen) throws IOException {
            service.customizedQuery(conditions, (T item, int index) -> {
                boolean write = filter != null ? filter.test(item) : true;
                if (write) {
                    count++;
                    try {
                        jgen.writeObject(toModel.apply(item));
                    } catch (IOException e) {
                        throw new GenericRestException(HttpStatus.INTERNAL_SERVER_ERROR, e);
                    }
                }
            });
        }
    }
}
