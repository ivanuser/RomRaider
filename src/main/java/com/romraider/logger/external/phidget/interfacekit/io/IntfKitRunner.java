/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2026 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.logger.external.phidget.interfacekit.io;

import static org.apache.log4j.Logger.getLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.romraider.logger.external.core.Stoppable;
import com.romraider.logger.external.phidget.interfacekit.plugin.IntfKitDataItem;

/**
 * IntfKitRunner polls the open PhidgetInterfaceKits for analog data and pushes
 * each changed value to the matching logger data item.
 *
 * <p>The old phidget21 wrapper delivered values through native sensor-change
 * callbacks; hid4java is read-based, so this polls each device and reports a
 * value only when it changes (matching the old change-trigger behaviour).</p>
 */
public final class IntfKitRunner implements Stoppable {
    private static final Logger LOGGER = getLogger(IntfKitRunner.class);
    private static final long POLL_INTERVAL_MS = 50L;

    private final Map<String, IntfKitDataItem> dataItems;
    private final Set<PhidgetIkDevice> connections;
    private final Map<String, Integer> lastValues = new HashMap<String, Integer>();
    private volatile boolean stop;

    /**
     * @param kits      serial numbers of the InterfaceKits to read
     * @param dataItems map of InterfaceKit data items keyed by "serial:sensor"
     */
    public IntfKitRunner(
            final List<Integer> kits,
            final Map<String, IntfKitDataItem> dataItems) {
        this.dataItems = dataItems;
        this.connections = IntfKitConnector.openIkSerial(kits);
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                for (PhidgetIkDevice device : connections) {
                    final int[] values = device.readSensorValues();
                    if (values == null) {
                        continue;
                    }
                    for (int sensor = 0; sensor < values.length; sensor++) {
                        updateDataItem(device.getSerial(), sensor, values[sensor]);
                    }
                }
                Thread.sleep(POLL_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            for (PhidgetIkDevice device : connections) {
                device.close();
            }
        }
    }

    @Override
    public void stop() {
        stop = true;
    }

    /**
     * Update a data item, but only when the sensor value actually changed.
     * @param serial serial number of the reporting InterfaceKit
     * @param sensor sensor index that changed
     * @param value  the new value
     */
    public void updateDataItem(final int serial, final int sensor, final int value) {
        if (serial == -1) {
            LOGGER.error("Phidget InterfaceKit dataitem update error");
            return;
        }
        final String inputName = String.format("%d:%d", serial, sensor);
        final Integer previous = lastValues.get(inputName);
        if (previous != null && previous == value) {
            return;
        }
        lastValues.put(inputName, value);
        final IntfKitDataItem item = dataItems.get(inputName);
        if (item != null) {
            item.setData(value);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format(
                        "Phidget InterfaceKit sensor %s event - raw value: %d",
                        inputName, value));
            }
        }
    }
}
