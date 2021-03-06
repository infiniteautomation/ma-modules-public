/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infiniteautomation.mango.rest.latest.model.thread.ThreadModel;
import com.infiniteautomation.mango.rest.latest.model.thread.ThreadModelProperty;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * @author Terry Packer
 *
 */
@Api(value="Mango Application Threads")
@RestController
@RequestMapping("/threads")
public class ThreadMonitorRestController {

    private final ThreadGroup root; //The root group, always will be there

    public ThreadMonitorRestController(){

        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        ThreadGroup ptg;
        while ( (ptg = tg.getParent( )) != null )
            tg = ptg;
        this.root = tg;

    }

    @ApiOperation(value = "Get all threads", notes = "Larger stack depth will slow this request")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ThreadModel>> getThreads(
            @ApiParam(value = "Limit size of stack trace", allowMultiple = false, defaultValue="10")
            @RequestParam(value="stackDepth", defaultValue="10") int stackDepth,
            @ApiParam(value = "Return as file", allowMultiple = false, defaultValue="false")
            @RequestParam(value="asFile", defaultValue="false") boolean asFile,
            @ApiParam(value = "Order by this member", allowMultiple = false, required=false)
            @RequestParam(value="orderBy", required=false) String orderBy
            ){

        HttpHeaders headers = new HttpHeaders();
        List<ThreadModel> models = new ArrayList<ThreadModel>();

        Thread[] allThreads = this.getAllThreads();
        ThreadMXBean manager = ManagementFactory.getThreadMXBean();
        for(Thread t : allThreads){
            ThreadInfo info = manager.getThreadInfo(t.getId(), stackDepth);
            ThreadModel model;
            if(info != null)
                model = new ThreadModel(t.getId(), t.getPriority(), t.getName(), info, manager.getThreadCpuTime(t.getId()), manager.getThreadUserTime(t.getId()));
            else
                model = new ThreadModel(t.getId(), t.getPriority(), t.getName(), manager.getThreadCpuTime(t.getId()), manager.getThreadUserTime(t.getId()));
            models.add(model);
        }
        //Do we need to order this list?
        if(orderBy != null){

            //Determine what to order by
            final ThreadModelProperty orderProperty = ThreadModelProperty.convert(orderBy);


            Collections.sort(models, new Comparator<ThreadModel>(){

                @Override
                public int compare(ThreadModel left, ThreadModel right) {
                    switch(orderProperty){
                        case PRIORITY:
                            return left.getPriority() - right.getPriority();
                        case NAME:
                            return left.getName().compareTo(right.getName());
                        case CPU_TIME:
                            if(left.getCpuTime() > right.getCpuTime())
                                return 1;
                            else if((left.getCpuTime() < right.getCpuTime()))
                                return -1;
                            else
                                return 0;
                        case USER_TIME:
                            if(left.getUserTime() > right.getUserTime())
                                return 1;
                            else if((left.getUserTime() < right.getUserTime()))
                                return -1;
                            else
                                return 0;
                        case STATE:
                            return left.getState().compareTo(right.getState());
                        case LOCATION:
                        case ID:
                        default:
                            return (int) (left.getId() - right.getId());
                    }

                }

            });
        }

        if(asFile){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss_'threads.json'");
            String filename = sdf.format(new Date());
            headers.setContentDisposition(ContentDisposition.parse("attachment;filename=" + filename));
        }
        return new ResponseEntity<>(models, headers, HttpStatus.OK);
    }


    private Thread[] getAllThreads( ) {
        final ThreadMXBean thbean = ManagementFactory.getThreadMXBean( );
        int nAlloc = thbean.getThreadCount( );
        int n = 0;
        Thread[] threads;
        do {
            nAlloc *= 2;
            threads = new Thread[ nAlloc ];
            n = root.enumerate( threads, true );
        } while ( n == nAlloc );
        return java.util.Arrays.copyOf( threads, n );
    }

}
