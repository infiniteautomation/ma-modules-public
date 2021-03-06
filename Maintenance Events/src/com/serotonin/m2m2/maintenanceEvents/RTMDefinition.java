/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.maintenanceEvents;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.serotonin.m2m2.module.RuntimeManagerDefinition;

public class RTMDefinition extends RuntimeManagerDefinition {
    public static RTMDefinition instance;

    private final List<MaintenanceEventRT> maintenanceEvents = new CopyOnWriteArrayList<MaintenanceEventRT>();

    public RTMDefinition() {
        instance = this;
    }

    @Override
    public int getInitializationPriority() {
        return 11;
    }

    @Override
    public void initialize(boolean safe) {
        for (MaintenanceEventVO vo : MaintenanceEventDao.getInstance().getAll()) {
            if (!vo.isDisabled()) {
                if (safe) {
                    vo.setDisabled(true);
                    MaintenanceEventDao.getInstance().update(vo.getId(), vo);
                }
                else
                    startMaintenanceEvent(vo);
            }
        }
    }

    @Override
    public void terminate() {
        while (!maintenanceEvents.isEmpty())
            stopMaintenanceEvent(maintenanceEvents.get(0).getVo().getId());
    }

    //
    //
    // Maintenance events
    //
    public MaintenanceEventRT getRunningMaintenanceEvent(int id) {
        for (MaintenanceEventRT rt : maintenanceEvents) {
            if (rt.getVo().getId() == id)
                return rt;
        }
        return null;
    }

    public boolean isActiveMaintenanceEventForDataSource(int dataSourceId) {
        for (MaintenanceEventRT rt : maintenanceEvents) {
            for(Integer dsId : rt.getVo().getDataSources())
                if (dsId == dataSourceId && rt.isEventActive())
                    return true;
        }
        return false;
    }

    public boolean isActiveMaintenanceEventForDataPoint(int dataPointId) {
        for (MaintenanceEventRT rt : maintenanceEvents) {
            for(Integer dpId : rt.getVo().getDataPoints())
                if (dpId == dataPointId && rt.isEventActive())
                    return true;
        }
        return false;
    }

    public boolean isMaintenanceEventRunning(int id) {
        return getRunningMaintenanceEvent(id) != null;
    }

    public void deleteMaintenanceEvent(int id) {
        stopMaintenanceEvent(id);
        MaintenanceEventDao.getInstance().delete(id);
    }

    public void saveMaintenanceEvent(MaintenanceEventVO vo) {
        // If the maintenance event is running, stop it.
        stopMaintenanceEvent(vo.getId());

        if(vo.isNew()) {
            MaintenanceEventDao.getInstance().insert(vo);
        }else {
            MaintenanceEventDao.getInstance().update(vo.getId(), vo);
        }

        // If the maintenance event is enabled, start it.
        if (!vo.isDisabled())
            startMaintenanceEvent(vo);
    }

    private void startMaintenanceEvent(MaintenanceEventVO vo) {
        synchronized (maintenanceEvents) {
            // If the maintenance event is already running, just quit.
            if (isMaintenanceEventRunning(vo.getId()))
                return;

            // Ensure that the maintenance event is enabled.
            if(vo.isDisabled())
                return;

            // Create and start the runtime version of the maintenance event.
            MaintenanceEventRT rt = new MaintenanceEventRT(vo);
            rt.initialize();

            // Add it to the list of running maintenance events.
            maintenanceEvents.add(rt);
        }
    }

    private void stopMaintenanceEvent(int id) {
        synchronized (maintenanceEvents) {
            MaintenanceEventRT rt = getRunningMaintenanceEvent(id);
            if (rt == null)
                return;

            maintenanceEvents.remove(rt);
            rt.terminate();
        }
    }
}
