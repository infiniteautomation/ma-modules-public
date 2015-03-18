/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.JsonArrayStream;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.EventInstanceDatabaseStream;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.EventModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.query.QueryModel;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * 
 * TODO Flesh out after we get the events endpoint rest api defined
 * 
 * @author Terry Packer
 *
 */
@Api(value="Events", description="Operations on Events")
@RestController()
@RequestMapping("/v1/events")
public class EventsRestController extends MangoRestController{
	
	//private static Log LOG = LogFactory.getLog(EventsRestController.class);
	
	public EventsRestController(){ }

	
	@ApiOperation(
			value = "Get all events",
			notes = "",
			response=EventModel.class,
			responseContainer="Array"
			)
	@ApiResponses(value = { 
			@ApiResponse(code = 200, message = "Ok", response=EventModel.class),
			@ApiResponse(code = 403, message = "User does not have access", response=ResponseEntity.class)
		})
	@RequestMapping(method = RequestMethod.GET, produces={"application/json"})
    public ResponseEntity<JsonArrayStream> getAll(HttpServletRequest request, 
    		@RequestParam(value="limit", required=false, defaultValue="100")Integer limit) {

        RestProcessResult<JsonArrayStream> result = new RestProcessResult<JsonArrayStream>(HttpStatus.OK);
        
        this.checkUser(request, result);
    	
        if(result.isOk()){
        	QueryModel query = new QueryModel();
        	query.setLimit(limit);
    		EventInstanceDatabaseStream eventDatabaseStream = new EventInstanceDatabaseStream(query);
    		return result.createResponseEntity(eventDatabaseStream);
    	}
        return result.createResponseEntity();
	}
	
	@ApiOperation(
			value = "Query Events",
			notes = "",
			response=EventModel.class,
			responseContainer="Array"
			)
	@ApiResponses(value = { 
			@ApiResponse(code = 200, message = "Ok", response=EventModel.class),
			@ApiResponse(code = 403, message = "User does not have access", response=ResponseEntity.class)
		})
	@RequestMapping(method = RequestMethod.POST, consumes={"application/json"}, produces={"application/json"}, value = "/query")
    public ResponseEntity<JsonArrayStream> query(
    		
    		@ApiParam(value="Query", required=true)
    		@RequestBody(required=true) QueryModel query, 
    		   		
    		HttpServletRequest request) {
		
		RestProcessResult<JsonArrayStream> result = new RestProcessResult<JsonArrayStream>(HttpStatus.OK);
    	this.checkUser(request, result);
    	if(result.isOk()){
    		EventInstanceDatabaseStream eventDatabaseStream = new EventInstanceDatabaseStream(query);
    		return result.createResponseEntity(eventDatabaseStream);
    	}
    	
    	return result.createResponseEntity();
	}

}
