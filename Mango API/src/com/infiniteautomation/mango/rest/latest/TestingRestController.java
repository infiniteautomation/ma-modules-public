/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

import com.infiniteautomation.mango.rest.latest.exception.GenericRestException;
import com.infiniteautomation.mango.rest.latest.model.RestModelMapper;
import com.infiniteautomation.mango.rest.latest.model.event.RaiseEventModel;
import com.infiniteautomation.mango.rest.latest.model.session.MangoSessionDataModel;
import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.infiniteautomation.mango.spring.components.executors.MangoExecutors;
import com.infiniteautomation.mango.spring.service.TestingService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.webapp.session.MangoSessionDataStore;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.LicenseViolatedException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.vo.MangoSessionDataVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionRegistry;
import com.serotonin.timer.RejectedTaskReason;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Jared Wiltshire
 */
@RestController
@ConditionalOnProperty(value = {"${testing.enabled:false}", "${testing.restApi.enabled:false}"})
@PreAuthorize("isAdmin()")
@RequestMapping("/testing")
public class TestingRestController {

    private final Logger log = LoggerFactory.getLogger(TestingRestController.class);

    private final Set<PosixFilePermission> readablePerms =
            new HashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ));

    private final MangoSessionRegistry sessionRegistry;
    private final MangoSessionDataStore sessionDataStore;
    private final RestModelMapper modelMapper;
    private final MangoExecutors executors;
    private final TestingService service;

    @Autowired
    public TestingRestController(final MangoSessionRegistry sessionRegistry,
                                 final MangoSessionDataStore sessionDataDao,
                                 final RestModelMapper modelMapper,
                                 final MangoExecutors executors,
                                 final TestingService service) {
        this.sessionRegistry = sessionRegistry;
        this.sessionDataStore = sessionDataDao;
        this.modelMapper = modelMapper;
        this.executors = executors;
        this.service = service;
    }

    @RequestMapping(method = {RequestMethod.GET}, value = "/location")
    public ResponseEntity<Void> testLocation(UriComponentsBuilder builder) {

        HttpHeaders headers = new HttpHeaders();
        URI location = builder.path("/{id}").buildAndExpand("over-here").toUri();
        headers.setLocation(location);

        return new ResponseEntity<>(null, headers, HttpStatus.CREATED);
    }

    @RequestMapping(method = {RequestMethod.GET}, value = "/remote-addr")
    public String testLocation(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    @RequestMapping(method = {RequestMethod.POST}, value = "/heap-dump")
    public String heapDump(@RequestParam String filename,
                           @RequestParam(required = false, defaultValue = "false") boolean overwrite,
                           @RequestParam(required = false, defaultValue = "false") boolean readable) throws Exception {
        boolean ibm = true;
        try {
            Class.forName("com.ibm.jvm.Dump");
        } catch (ClassNotFoundException e) {
            ibm = false;
        }

        Path filePath;

        if (ibm) {
            filePath = Common.MA_HOME_PATH.resolve(filename + ".phd").toAbsolutePath();
        } else {
            filePath = Common.MA_HOME_PATH.resolve(filename + ".hprof").toAbsolutePath();
        }

        log.info("Dumping heap to {}", filePath);

        if (overwrite) {
            Files.deleteIfExists(filePath);
        }

        if (ibm) {
            String newPath = (String) Class.forName("com.ibm.jvm.Dump").getMethod("heapDumpToFile", String.class).invoke(null, filePath.toString());
            filePath = Paths.get(newPath).toAbsolutePath();
        } else {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            Class<?> clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
            Object bean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", clazz);
            clazz.getMethod("dumpHeap", String.class, boolean.class).invoke(bean, filePath.toString(), true);
        }

        // the dumps are written with only user read perms, enable relaxing the permissions
        if (readable) {
            try {
                Files.setPosixFilePermissions(filePath, readablePerms);
            } catch (UnsupportedOperationException e) {
                // dont care
            }
        }

        return filePath.toString();
    }

    @RequestMapping(method = {RequestMethod.GET}, value = "/jvm-info")
    public JVMInfo jvmInfo() {
        return new JVMInfo();
    }

    public static class JVMInfo {
        final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        public String getName() {
            return runtime.getName();
        }

        public long getUptime() {
            return runtime.getUptime();
        }

        public long getStartTime() {
            return runtime.getStartTime();
        }

        public String getVmName() {
            return runtime.getVmName();
        }

        public String getVmVendor() {
            return runtime.getVmVendor();
        }

        public String getVmVersion() {
            return runtime.getVmVersion();
        }

        public String getSpecName() {
            return runtime.getSpecName();
        }

        public String getSpecVendor() {
            return runtime.getSpecVendor();
        }

        public String getSpecVersion() {
            return runtime.getSpecVersion();
        }
    }

    @ApiOperation(value = "Example User Credentials test")
    @ApiResponses({
            @ApiResponse(code = 401, message = "Unauthorized user access", response = ResponseEntity.class),
    })
    @RequestMapping(method = {RequestMethod.GET}, value = {"/admin-get/{resourceId}"})
    public ResponseEntity<Object> exampleGet(@ApiParam(value = "Resource id", required = true) @PathVariable String resourceId) {
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @ApiOperation(value = "Example User Credentials test")
    @ApiResponses({
            @ApiResponse(code = 401, message = "Unauthorized user access", response = ResponseEntity.class),
    })
    @RequestMapping(method = {RequestMethod.GET}, value = {"/user-get/{resourceId}"})
    public ResponseEntity<Object> userGet(@ApiParam(value = "Resource id", required = true) @PathVariable String resourceId) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Example Permission Exception Response")
    @ApiResponses({
            @ApiResponse(code = 401, message = "Unauthorized user access", response = ResponseEntity.class),
    })
    @RequestMapping(method = {RequestMethod.GET}, value = {"/permissions-exception"})
    public ResponseEntity<Object> alwaysFails(@AuthenticationPrincipal PermissionHolder user) {
        throw new PermissionException(new TranslatableMessage("common.default", "I always fail."), user);
    }

    @ApiOperation(value = "Example Access Denied Exception Response")
    @ApiResponses({
            @ApiResponse(code = 401, message = "Unauthorized user access", response = ResponseEntity.class),
    })
    @RequestMapping(method = {RequestMethod.GET}, value = {"/access-denied-exception"})
    public ResponseEntity<Object> accessDenied() {
        throw new AccessDeniedException("I don't have access.");
    }

    @ApiOperation(value = "Example Generic Rest Exception Response")
    @ApiResponses({
            @ApiResponse(code = 401, message = "Unauthorized user access", response = ResponseEntity.class),
    })
    @RequestMapping(method = {RequestMethod.GET}, value = {"/generic-exception"})
    public ResponseEntity<Object> genericFailure() {
        throw new GenericRestException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ApiOperation(value = "Example Runtime Exception Response")
    @ApiResponses({
            @ApiResponse(code = 401, message = "Unauthorized user access", response = ResponseEntity.class),
    })
    @RequestMapping(method = {RequestMethod.GET}, value = {"/runtime-exception"})
    public ResponseEntity<Object> runtimeFailure() {
        throw new RuntimeException("I'm a runtime Exception");
    }

    @ApiOperation(value = "Example IOException Response")
    @ApiResponses({
            @ApiResponse(code = 401, message = "Unauthorized user access", response = ResponseEntity.class),
    })
    @RequestMapping(method = {RequestMethod.GET}, value = {"/io-exception"})
    public ResponseEntity<Object> ioFailure() throws IOException {
        throw new IOException("I'm an Exception");
    }

    @ApiOperation(value = "Example LicenseViolationException Response")
    @ApiResponses({
            @ApiResponse(code = 401, message = "Unauthorized user access", response = ResponseEntity.class),
    })
    @RequestMapping(method = {RequestMethod.GET}, value = {"/license-violation"})
    public ResponseEntity<Object> licenseViolation() throws IOException {
        throw new LicenseViolatedException(new TranslatableMessage("common.default", "Test Violiation"));
    }

    @ApiOperation(value = "Expire the session of the current user", notes = "must be admin")
    @ApiResponses({
            @ApiResponse(code = 401, message = "Unauthorized user access", response = ResponseEntity.class),
    })
    @RequestMapping(method = {RequestMethod.GET}, value = {"/expire-session"})
    public ResponseEntity<Object> expireSessions(@AuthenticationPrincipal PermissionHolder user) {
        List<SessionInformation> infos = sessionRegistry.getAllSessions(user, false);
        for (SessionInformation info : infos)
            info.expireNow();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Example Path matching")
    @ApiResponses({
            @ApiResponse(code = 401, message = "Unauthorized user access", response = ResponseEntity.class),
    })
    @RequestMapping(method = {RequestMethod.GET}, value = {"/{resourceId}/**"})
    public ResponseEntity<String> matchPath(@ApiParam(value = "Resource id", required = true) @PathVariable String resourceId,
                                            HttpServletRequest request) {

        String path = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        AntPathMatcher apm = new AntPathMatcher();
        String finalPath = apm.extractPathWithinPattern(bestMatchPattern, path);

        return new ResponseEntity<>(finalPath, HttpStatus.OK);
    }

    @ApiOperation(value = "Raise an event", notes = "must be admin")
    @ApiResponses({
            @ApiResponse(code = 401, message = "Unauthorized user access", response = ResponseEntity.class),
    })
    @RequestMapping(method = {RequestMethod.POST}, value = {"/raise-event"})
    public ResponseEntity<Object> raiseExampleEvent(@RequestBody RaiseEventModel model) {
        if (model == null)
            throw new GenericRestException(HttpStatus.INTERNAL_SERVER_ERROR);
        Common.eventManager.raiseEvent(model.getEvent().toVO(), Common.timer.currentTimeMillis(), true, model.getLevel(), new TranslatableMessage("common.default", model.getMessage()), model.getContext());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Identity function", notes = "Returns whatever is sent in the request body. Useful for testing message converters. Must be admin")
    @RequestMapping(method = RequestMethod.POST, value = {"/identity"})
    public Object identityFunction(
            @RequestBody Object node) {

        return node;
    }

    @ApiOperation(value = "Log ERROR Level Message", notes = "Must be admin")
    @RequestMapping(method = RequestMethod.POST, value = {"/log-error-message"})
    public void logErorMessage(
            @RequestBody String message) {
        log.error(message);
    }

    @ApiOperation(value = "Log WARN Level Message", notes = "Must be admin")
    @RequestMapping(method = RequestMethod.POST, value = {"/log-warn-message"})
    public void logWarnMessage(
            @RequestBody String message) {
        log.warn(message);
    }

    @ApiOperation(value = "Log INFO Level Message", notes = "Must be admin")
    @RequestMapping(method = RequestMethod.POST, value = {"/log-info-message"})
    public void logInfoMessage(
            @RequestBody String message) {
        log.info(message);
    }

    @ApiOperation(value = "Log DEBUG Level Message", notes = "Must be admin")
    @RequestMapping(method = RequestMethod.POST, value = {"/log-debug-message"})
    public void logDebugMessage(
            @RequestBody String message) {
        log.debug(message);
    }

    @ApiOperation(value = "Get upload limit")
    @RequestMapping(method = RequestMethod.GET, value = {"/upload-limit"})
    public long getUploadLimit() {
        return Common.envProps.getLong("web.fileUpload.maxSize", 50000000);
    }

    @Async
    @RequestMapping(method = RequestMethod.GET, value = {"/async-response"})
    public CompletableFuture<Double> delayedResponse() {
        return CompletableFuture.supplyAsync(Math::random);
    }

    @Async
    @ApiOperation(value = "Execute a long running request that eventually returns OK")
    @RequestMapping(method = RequestMethod.GET, value = {"/delay-response/{delayMs}"})
    public CompletableFuture<String> delayedResponse(
            @ApiParam(value = "Delay ms", required = true) @PathVariable int delayMs) throws InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayMs);
                return "OK";
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Async
    @ApiOperation(value = "Execute a long running request that eventually fails on a runtime exception")
    @RequestMapping(method = RequestMethod.GET, value = {"/async-failure/{delayMs}"})
    public CompletableFuture<String> asyncFailure(
            @ApiParam(value = "Delay ms", required = true) @PathVariable int delayMs) throws InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayMs);
                throw new CompletionException(new RuntimeException("I Should Fail"));
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
        });
    }

    @ApiOperation(value = "Get a persistent session entry")
    @RequestMapping(method = RequestMethod.GET, value = {"/persistent-session/{sessionId}"})
    public ResponseEntity<MangoSessionDataModel> getPersistentSession(
            @PathVariable
                    String sessionId,
            @RequestParam(required = false)
                    String contextPath,
            @RequestParam(required = false)
                    String virtualHost,
            @AuthenticationPrincipal PermissionHolder user) {

        if (contextPath == null) {
            contextPath = sessionDataStore.getSessionContext().getCanonicalContextPath();
        }
        if (virtualHost == null) {
            virtualHost = sessionDataStore.getSessionContext().getVhost();
        }
        MangoSessionDataVO vo = sessionDataStore.get(sessionId, contextPath, virtualHost);
        if (vo == null) {
            throw new NotFoundException();
        }
        return new ResponseEntity<>(modelMapper.map(vo, MangoSessionDataModel.class, user), HttpStatus.OK);
    }


    @ApiOperation(value = "Create a persistent session entry")
    @RequestMapping(method = RequestMethod.POST, value = {"/persistent-session"})
    public ResponseEntity<MangoSessionDataModel> insertPersistentSession(
            @RequestBody
                    MangoSessionDataModel model,
            @AuthenticationPrincipal PermissionHolder user,
            HttpServletRequest request) {

        //Fill in some helpful pieces if they are missing
        if (model.getContextPath() == null) {
            model.setContextPath(sessionDataStore.getSessionContext().getCanonicalContextPath());
        }
        if (model.getVirtualHost() == null) {
            model.setVirtualHost(sessionDataStore.getSessionContext().getVhost());
        }
        HttpSession session = request.getSession(false);
        if (model.getLastAccessTime() == null) {
            model.setLastAccessTime(new Date(session.getLastAccessedTime()));
        }
        if (model.getCreateTime() == null) {
            model.setCreateTime(new Date(session.getCreationTime()));
        }

        sessionDataStore.add(modelMapper.unMap(model, MangoSessionDataVO.class, user));

        return new ResponseEntity<>(model, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Update a persistent session entry")
    @RequestMapping(method = RequestMethod.PUT, value = {"/persistent-session/{sessionId}"})
    public ResponseEntity<MangoSessionDataModel> updatePersistentSession(
            @PathVariable
                    String sessionId,
            @RequestBody
                    MangoSessionDataModel model,
            @AuthenticationPrincipal PermissionHolder user,
            HttpServletRequest request) {

        //Fill in some helpful pieces if they are missing
        if (model.getContextPath() == null) {
            model.setContextPath(sessionDataStore.getSessionContext().getCanonicalContextPath());
        }
        if (model.getVirtualHost() == null) {
            model.setVirtualHost(sessionDataStore.getSessionContext().getVhost());
        }
        HttpSession session = request.getSession(false);
        if (model.getLastAccessTime() == null) {
            model.setLastAccessTime(new Date(session.getLastAccessedTime()));
        }
        if (model.getCreateTime() == null) {
            model.setCreateTime(new Date(session.getCreationTime()));
        }

        sessionDataStore.update(sessionId,
                model.getContextPath(), model.getVirtualHost(),
                modelMapper.unMap(model, MangoSessionDataVO.class, user));

        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @ApiOperation(value = "Delete a persistent session entry")
    @RequestMapping(method = RequestMethod.DELETE, value = {"/persistent-session/{sessionId}"})
    public ResponseEntity<Void> deletePersistentSession(
            @PathVariable
                    String sessionId,
            @RequestParam(required = false)
                    String contextPath,
            @RequestParam(required = false)
                    String virtualHost) {

        if (contextPath == null) {
            contextPath = sessionDataStore.getSessionContext().getCanonicalContextPath();
        }
        if (virtualHost == null) {
            virtualHost = sessionDataStore.getSessionContext().getVhost();
        }
        if (sessionDataStore.delete(sessionId, contextPath, virtualHost)) {
            throw new NotFoundException();
        }

        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @ApiOperation(value = "Execute a real time timer task that will consume all Mango's memory")
    @RequestMapping(method = RequestMethod.GET, value = {"/real-time-timer/schedule/oom"})
    public void TimerTaskOom() {
        new TimeoutTask(100, new TimeoutClient() {
            @Override
            public void scheduleTimeout(long fireTime) {
                consumeAllMemory();
            }

            @Override
            public String getThreadName() {
                return "Mango Real Time Timer Memory Consumer";
            }
        });
    }

    @ApiOperation(value = "Execute a work item at the supplied priority that will consume all Mango's memory [HIGH, MEDIUM, LOW]")
    @RequestMapping(method = RequestMethod.GET, value = {"/work-items/{priority}/schedule/oom"})
    public void workItemOom(
            @PathVariable String priority
    ) {

        int priorityValue;
        switch(priority) {
            case "HIGH":
                priorityValue = WorkItem.PRIORITY_HIGH;
            break;
            case "MEDIUM":
                priorityValue = WorkItem.PRIORITY_MEDIUM;
                break;
            default:
            case "LOW":
                priorityValue = WorkItem.PRIORITY_LOW;
        }

        WorkItem item = new WorkItem() {
            @Override
            public void execute() {
                consumeAllMemory();
            }

            @Override
            public int getPriority() {
                return priorityValue;
            }

            @Override
            public String getDescription() {
                return "Mango memory eater";
            }

            @Override
            public void rejected(RejectedTaskReason reason) {

            }
        };
        Common.backgroundProcessing.addWorkItem(item);
    }

    @ApiOperation(value = "Fire a Spring event that will consume all Mango's memory")
    @RequestMapping(method = RequestMethod.GET, value = {"/spring/event/oom"})
    public void SpringEventOom() {
        this.service.generateRunnableEvent(() -> consumeAllMemory());
    }

    @ApiOperation(value = "Execute a task in MangoExecutors.executor that will consume all Mango's memory")
    @RequestMapping(method = RequestMethod.GET, value = {"/executors/executor/oom"})
    public void MangoExecutorsExecutorOom() {
        this.executors.getExecutor().execute(() -> consumeAllMemory());
    }

    @ApiOperation(value = "Schedule a task in MangoExecutors.scheduledExecutor that will consume all Mango's memory")
    @RequestMapping(method = RequestMethod.GET, value = {"/executors/scheduledExecutor/oom"})
    public void MangoExecutorsScheduledExecutorOom() {
        ScheduledFuture<?> future = this.executors.getScheduledExecutor().schedule(() -> consumeAllMemory(), 1 , TimeUnit.SECONDS);
        try {
            future.get();
        }catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @ApiOperation(value = "Schedule a task in MangoExecutors.superadminExecutor that will consume all Mango's memory")
    @RequestMapping(method = RequestMethod.GET, value = {"/executors/superadminExecutor/oom"})
    public void MangoExecutorsSuperadminExecutorOom() {
        this.executors.getSuperadminExecutor().execute(() -> consumeAllMemory());
    }

    @ApiOperation(value = "Schedule a task in MangoExecutors.superadminScheduledExecutor that will consume all Mango's memory")
    @RequestMapping(method = RequestMethod.GET, value = {"/executors/superadminScheduledExecutor/oom"})
    public void MangoExecutorsSuperadminScheduledExecutorOom() {
        this.executors.getSuperadminScheduledExecutor().schedule(() -> consumeAllMemory(), 1 , TimeUnit.SECONDS);
    }

    @ApiOperation(value = "Make a REST call that will consume all Mango's memory")
    @RequestMapping(method = RequestMethod.GET, value = {"/oom"})
    public void restOom() {
        consumeAllMemory();
    }

    /**
     * Fill a list with arrays until we run out of memory
     */
    private void consumeAllMemory() {
        List<byte[]> allMyMemory = new ArrayList<>();
        while(true) {
            byte[] b = new byte[1048567];
            allMyMemory.add(b);
        }
    }
}
